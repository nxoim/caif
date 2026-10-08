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

/**
 * Built-in position, participation, rendering, and layout
 * defaults share [stackOrder].
 */
@Composable
@JvmName("rememberStackAnimatorStateStackItemContext")
fun <ItemType, Key : Any> rememberStackAnimatorState(
    stack: State<List<ItemType>>,
    keyFor: (ItemType) -> Key,
    factory: ItemAnimationFactory<ItemType, Key, StackItemPosition>,
    stackOrder: StackOrder = StackOrder.TopmostLast,
    affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, StackItemPosition> =
        remember(stackOrder) { AffectedItemsPolicy.fromTop(stackOrder) },
    maxAffected: Int = Int.MAX_VALUE,
    scope: CoroutineScope = rememberCoroutineScope(),
    renderOrder: RenderOrderStrategy<Key> = remember(stackOrder) {
        RenderOrderStrategy.byStackIndex(stackOrder)
    }
): StackAnimatorState<ItemType, Key, StackItemPosition> =
    rememberStackAnimatorState(
        stack = stack,
        keyFor = keyFor,
        stackOrder = stackOrder,
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
                    stackOrder.depthFromTop(indexOf(it), stackSnapshot.lastIndex),
                    previousSnapshot?.let { previous ->
                        previousIndexOf(it)?.let { index ->
                            stackOrder.depthFromTop(index, previous.lastIndex)
                        }
                    },
                )
            }
        }
    )

/**
 * Custom contexts must interpret their snapshot order consistently with [stackOrder].
 */
@Composable
fun <ItemType, Key : Any, Context> rememberStackAnimatorState(
    stack: State<List<ItemType>>,
    keyFor: (ItemType) -> Key,
    factory: ItemAnimationFactory<ItemType, Key, Context>,
    contextFactory: ContextFactory<ItemType, Context, StackCreationContext<ItemType>>,
    stackOrder: StackOrder = StackOrder.TopmostLast,
    affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context> =
        remember(stackOrder) { AffectedItemsPolicy.fromTop(stackOrder) },
    maxAffected: Int = Int.MAX_VALUE,
    scope: CoroutineScope = rememberCoroutineScope(),
    renderOrder: RenderOrderStrategy<Key> = remember(stackOrder) {
        RenderOrderStrategy.byStackIndex(stackOrder)
    }
) = rememberStackAnimatorState(
    stack = stack,
    factory = factory,
    stackOrder = stackOrder,
    resolver = rememberStackContextResolver(
        contextFactory,
        keyFor
    ),
    affectedItemsPolicy = affectedItemsPolicy,
    maxAffected = maxAffected,
    scope = scope,
    renderOrder = renderOrder
)

@Composable
fun <ItemType, Key : Any, Context, CreationContext> rememberStackAnimatorState(
    stack: State<List<ItemType>>,
    factory: ItemAnimationFactory<ItemType, Key, Context>,
    resolver: ContextResolver<ItemType, Key, Context, CreationContext>,
    stackOrder: StackOrder = StackOrder.TopmostLast,
    affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context> =
        remember(stackOrder) { AffectedItemsPolicy.fromTop(stackOrder) },
    maxAffected: Int = Int.MAX_VALUE,
    scope: CoroutineScope = rememberCoroutineScope(),
    renderOrder: RenderOrderStrategy<Key> = remember(stackOrder) {
        RenderOrderStrategy.byStackIndex(stackOrder)
    }
): StackAnimatorState<ItemType, Key, Context> {
    val currentFactory = rememberUpdatedState(factory)
    val currentResolver = rememberUpdatedState(resolver)
    val currentPolicy = rememberUpdatedState(affectedItemsPolicy)
    val currentRenderOrder = rememberUpdatedState(renderOrder)

    val state = remember(stack, maxAffected, scope, stackOrder) {
        val registry = ItemAnimationRegistry.fromProvider { currentFactory.value }
        val delegatingResolver = DelegatingContextResolver { currentResolver.value }

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

        StackAnimatorStateImpl(orchestrator, stackOrder)
    }

    DisposableEffect(state) {
        onDispose { state.dispose() }
    }
    return state
}

interface StackAnimatorState<ItemType, Key : Any, Context> {
    /**
     * Snapshot ordering used by the built-in layout and
     * transition-retention defaults.
     */
    val stackOrder: StackOrder

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

inline fun <reified D : CapabilityDispatcher> StackAnimatorState<*, *, *>.getOrCreateDispatcher(
    noinline factory: (StackCycleController) -> D
): D = getOrCreateDispatcher(kClass = D::class, factory)

fun <Context, ItemType, Key : Any> defaultStackContextResolver(
    contextFactory: ContextFactory<ItemType, Context, StackCreationContext<ItemType>>,
    keyFor: (ItemType) -> Key
): ContextResolver<ItemType, Key, Context, StackCreationContext<ItemType>> =
    DefaultStackContextResolver(contextFactory, keyFor)

class StackAnimatorStateImpl<ItemType, Key : Any, Context, CreationContext>(
    private val orchestrator: StackOrchestrator<ItemType, Key, Context, CreationContext>,
    override val stackOrder: StackOrder = StackOrder.TopmostLast,
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

private class DefaultStackContextResolver<ItemType, Key : Any, Context>(
    private val contextFactory: ContextFactory<ItemType, Context, StackCreationContext<ItemType>>,
    private val keyForItem: (ItemType) -> Key,
) : ContextResolver<ItemType, Key, Context, StackCreationContext<ItemType>> {
    override fun keyFor(itemType: ItemType): Key = keyForItem(itemType)

    override fun buildContexts(
        stack: List<ItemType>,
        previousStack: List<ItemType>?,
        treatNewEnteringAsPreparing: Boolean,
        recalculateEnteringToMoving: Boolean,
        previousContexts: Map<Key, Context>?
    ): Map<Key, Pair<Context, StackCreationContext<ItemType>>> {
        val previousKeys = previousStack?.let { MutableScatterSet<Key>(it.size) }
        val currentKeys = MutableScatterSet<Key>(stack.size)
        val itemsByKey = LinkedHashMap<Key, ItemType>(
            (previousStack?.size ?: 0) + stack.size
        )
        previousStack?.fastForEach { item ->
            val key = keyFor(item)
            previousKeys?.add(key)
            itemsByKey[key] = item
        }
        stack.fastForEach { item ->
            val key = keyFor(item)
            currentKeys.add(key)
            itemsByKey[key] = item
        }

        val indexLookup = StackCreationContextIndexLookup(stack, previousStack)
        val result =
            LinkedHashMap<Key, Pair<Context, StackCreationContext<ItemType>>>(itemsByKey.size)
        itemsByKey.forEach { (key, item) ->
            val isInPrevious = previousKeys?.contains(key) == true
            val isInCurrent = key in currentKeys

            val intention = when {
                !isInCurrent -> AppearanceIntention.Removal
                treatNewEnteringAsPreparing && !isInPrevious -> AppearanceIntention.Entrance
                else -> AppearanceIntention.Movement
            }

            val creationContext = StackCreationContext(
                stackSnapshot = stack,
                previousSnapshot = previousStack,
                intention = intention
            ).apply { installIndexLookup(indexLookup) }
            result[key] = contextFactory.create(creationContext, item) to creationContext
        }
        return result
    }
}

private class DelegatingContextResolver<ItemType, Key : Any, Context, CreationContext>(
    private val currentResolver: () -> ContextResolver<ItemType, Key, Context, CreationContext>,
) : ContextResolver<ItemType, Key, Context, CreationContext> {
    override fun keyFor(itemType: ItemType): Key =
        currentResolver().keyFor(itemType)

    override fun buildContexts(
        stack: List<ItemType>,
        previousStack: List<ItemType>?,
        treatNewEnteringAsPreparing: Boolean,
        recalculateEnteringToMoving: Boolean,
        previousContexts: Map<Key, Context>?
    ): Map<Key, Pair<Context, CreationContext>> = currentResolver().buildContexts(
        stack,
        previousStack,
        treatNewEnteringAsPreparing,
        recalculateEnteringToMoving,
        previousContexts
    )
}
