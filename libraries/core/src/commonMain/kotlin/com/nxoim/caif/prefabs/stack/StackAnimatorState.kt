@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.caif.prefabs.stack

import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.util.fastForEach
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory
import com.nxoim.caif.utils.typeMap
import kotlinx.coroutines.CoroutineScope
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

@Composable
@JvmName("rememberStackAnimatorStateStackItemContext")
fun <ItemType, Key : Any> rememberStackAnimatorState(
    stack: State<List<ItemType>>,
    keyFor: (ItemType) -> Key,
    factory: ItemAnimationFactory<ItemType, Key, StackItemPosition>,
    affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, StackItemPosition> =
        remember { AffectedItemsPolicy.fromTop() },
    maxAffected: Int = Int.MAX_VALUE,
    scope: CoroutineScope = rememberCoroutineScope(),
    renderOrder: RenderOrderStrategy<Key> = remember { RenderOrderStrategy.insertionOrder() }
): StackAnimatorState<ItemType, Key, StackItemPosition> =
    rememberStackAnimatorState(
        stack = stack,
        keyFor = keyFor,
        maxAffected = maxAffected,
        factory = factory,
        affectedItemsPolicy = affectedItemsPolicy,
        scope = scope,
        renderOrder = renderOrder,
        contextFactory = {
            when (this.intention) {
                AppearanceIntention.Entrance -> StackItemPosition.PreEntered
                AppearanceIntention.Removal -> StackItemPosition.Removed
                else -> StackItemPosition.Inside(
                    indexOf(it),
                    previousIndexOf(it)
                )
            }
        }
    )

@Composable
fun <ItemType, Key : Any, Context> rememberStackAnimatorState(
    stack: State<List<ItemType>>,
    keyFor: (ItemType) -> Key,
    factory: ItemAnimationFactory<ItemType, Key, Context>,
    contextFactory: ContextFactory<ItemType, Context, StackCreationContext<ItemType>>,
    affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context> =
        remember { AffectedItemsPolicy.fromTop() },
    maxAffected: Int = Int.MAX_VALUE,
    scope: CoroutineScope = rememberCoroutineScope(),
    renderOrder: RenderOrderStrategy<Key> = remember { RenderOrderStrategy.insertionOrder() }
) = rememberStackAnimatorState(
    stack = stack,
    factory = factory,
    resolver = rememberStackContextResolver(
        contextFactory,
        keyFor
    ),
    affectedItemsPolicy = affectedItemsPolicy,
    maxAffected = maxAffected,
    scope = scope,
    renderOrder = renderOrder
)

fun <Context, ItemType, Key : Any> defaultStackContextResolver(
    contextFactory: ContextFactory<ItemType, Context, StackCreationContext<ItemType>>,
    keyFor: (ItemType) -> Key
): ContextResolver<ItemType, Key, Context, StackCreationContext<ItemType>> =
    object :
        ContextResolver<ItemType, Key, Context, StackCreationContext<ItemType>> {
        override fun keyFor(itemType: ItemType): Key = keyFor(itemType)

        override fun buildContexts(
            stack: List<ItemType>,
            previousStack: List<ItemType>?,
            treatNewEnteringAsPreparing: Boolean,
            recalculateEnteringToMoving: Boolean,
            previousContexts: Map<Key, Context>?
        ): Map<Key, Pair<Context, StackCreationContext<ItemType>>> {
            val previousKeys = previousStack?.let { list ->
                MutableScatterSet<Key>(list.size).apply { list.fastForEach { add(keyFor(it)) } }
            }
            val currentKeys = MutableScatterSet<Key>(stack.size).apply { stack.fastForEach { add(keyFor(it)) } }
            val itemsByKey = LinkedHashMap<Key, ItemType>(
                (previousStack?.size ?: 0) + stack.size
            )
            previousStack?.fastForEach { item -> itemsByKey[keyFor(item)] = item }
            stack.fastForEach { item -> itemsByKey[keyFor(item)] = item }

            val result = LinkedHashMap<Key, Pair<Context, StackCreationContext<ItemType>>>(itemsByKey.size)
            itemsByKey.forEach { (key, item) ->
                val isInPrevious = previousKeys?.contains(key) == true
                val isInCurrent = key in currentKeys

                val intention = when {
                    !isInCurrent -> AppearanceIntention.Removal
                    treatNewEnteringAsPreparing && !isInPrevious -> AppearanceIntention.Entrance
                    else -> AppearanceIntention.Movement
                }

                val creationContext =
                    StackCreationContext(
                        stackSnapshot = stack,
                        previousSnapshot = previousStack,
                        intention = intention
                    )
                result[key] = contextFactory.create(creationContext, item) to creationContext
            }
            return result
        }
    }

@Composable
private fun <Context, ItemType, Key : Any> rememberStackContextResolver(
    contextFactory: ContextFactory<ItemType, Context, StackCreationContext<ItemType>>,
    keyFor: (ItemType) -> Key
): ContextResolver<ItemType, Key, Context, StackCreationContext<ItemType>> {
    val currentContextFactory = rememberUpdatedState(contextFactory)
    val currentKeyFor = rememberUpdatedState(keyFor)
    return remember {
        defaultStackContextResolver(
            contextFactory = { item -> currentContextFactory.value.create(this, item) },
            keyFor = { item -> currentKeyFor.value(item) }
        )
    }
}

@Composable
fun <ItemType, Key : Any, Context, CreationContext> rememberStackAnimatorState(
    stack: State<List<ItemType>>,
    factory: ItemAnimationFactory<ItemType, Key, Context>,
    resolver: ContextResolver<ItemType, Key, Context, CreationContext>,
    affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context> =
        remember { AffectedItemsPolicy.fromTop() },
    maxAffected: Int = Int.MAX_VALUE,
    scope: CoroutineScope = rememberCoroutineScope(),
    renderOrder: RenderOrderStrategy<Key> = remember { RenderOrderStrategy.insertionOrder() }
): StackAnimatorState<ItemType, Key, Context> {
    val currentFactory = rememberUpdatedState(factory)
    val currentResolver = rememberUpdatedState(resolver)
    val currentPolicy = rememberUpdatedState(affectedItemsPolicy)
    val currentRenderOrder = rememberUpdatedState(renderOrder)

    val state = remember(stack, maxAffected, scope) {
        val registry = ItemAnimationRegistry<ItemType, Key, Context> { item, key ->
            currentFactory.value.create(item, key)
        }
        val delegatingResolver = object : ContextResolver<ItemType, Key, Context, CreationContext> {
            override fun keyFor(itemType: ItemType): Key =
                currentResolver.value.keyFor(itemType)

            override fun buildContexts(
                stack: List<ItemType>,
                previousStack: List<ItemType>?,
                treatNewEnteringAsPreparing: Boolean,
                recalculateEnteringToMoving: Boolean,
                previousContexts: Map<Key, Context>?
            ): Map<Key, Pair<Context, CreationContext>> =
                currentResolver.value.buildContexts(
                    stack,
                    previousStack,
                    treatNewEnteringAsPreparing,
                    recalculateEnteringToMoving,
                    previousContexts
                )
        }
        val orchestrator = StackOrchestrator(
            scope = scope,
            stack = stack,
            registry = registry,
            resolver = delegatingResolver,
            affectedItemsPolicy = { st, k, isVis, curCtx, tgtCtx, prevCtx, maxAff, minAff ->
                currentPolicy.value.selectAffectedItems(st, k, isVis, curCtx, tgtCtx, prevCtx, maxAff, minAff)
            },
            maxAffected = maxAffected,
            renderOrder = { activeKeys, stackOrderKeys ->
                currentRenderOrder.value.order(activeKeys, stackOrderKeys)
            }
        )

        StackAnimatorStateImpl(orchestrator)
    }

    DisposableEffect(state) {
        onDispose { state.dispose() }
    }
    return state
}

inline fun <reified D : CapabilityDispatcher> StackAnimatorState<*, *, *>.getOrCreateDispatcher(
    noinline factory: (StackCycleController) -> D
): D = getOrCreateDispatcher(kClass = D::class, factory)

interface StackAnimatorState<ItemType, Key : Any, Context> {
    fun <D : CapabilityDispatcher> getOrCreateDispatcher(
        kClass: KClass<D>,
        factory: (StackCycleController) -> D
    ): D

    /**
     * Registers an animation outside the item animation that must finish before [key] can be
     * removed from rendering. Multiple registrations for one key are all observed.
     *
     * The caller owns the returned registration and must unregister it when the external animation
     * leaves composition or should no longer retain the item. [isRunning] must read observable
     * Compose snapshot state so changes can resume a suspended item animation.
     */
    fun registerExternalAnimation(
        key: Key,
        isRunning: () -> Boolean
    ): ExternalAnimationRegistration

    val itemsToRender: List<Pair<Pair<Key, ItemType>, ItemAnimation<Context>>>
    /** Keys in the logical stack snapshot most recently processed by the animator. */
    val targetStackKeys: List<Key>
}

class StackAnimatorStateImpl<ItemType, Key : Any, Context, CreationContext>(
    private val orchestrator: StackOrchestrator<ItemType, Key, Context, CreationContext>,
) : StackAnimatorState<ItemType, Key, Context> {
    private val dispatcherStore = typeMap()
    override val itemsToRender get() = orchestrator.itemsToRender
    override val targetStackKeys get() = orchestrator.targetStackKeys

    override fun <D : CapabilityDispatcher> getOrCreateDispatcher(
        kClass: KClass<D>,
        factory: (StackCycleController) -> D
    ): D = dispatcherStore.getOrPut(kClass) { factory(orchestrator) }

    override fun registerExternalAnimation(
        key: Key,
        isRunning: () -> Boolean
    ): ExternalAnimationRegistration = orchestrator.registerExternalAnimation(key, isRunning)

    internal fun dispose() = orchestrator.dispose()
}
