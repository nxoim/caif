@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.caif.prefabs.stack

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.SelectableItemAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

class StackOrchestrator<ItemType, Key : Any, Context, CreationContext> : StackCycleController {
    constructor(
        scope: CoroutineScope,
        stack: State<List<ItemType>>,
        registry: ItemAnimationRegistry<ItemType, Key, Context>,
        resolver: ContextResolver<ItemType, Key, Context, CreationContext>,
        affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context> = AffectedItemsPolicy.fromTop(),
        maxAffected: Int,
        renderOrder: RenderOrderStrategy<Key>
    ) {
        this.scope = scope
        this.stack = stack
        this.registry = registry
        this.resolver = resolver
        this.affectedItemsPolicy = affectedItemsPolicy
        this.maxAffected = maxAffected
        this.renderOrder = renderOrder
        require(maxAffected >= 0) { "maxAffected must not be negative." }
        this.cycleState = StackCycleState(resolver)
        this.externalAnimations = ExternalAnimationRegistry()
        this.lastStackActedUpon = emptyList()
        this.keysCurrentlyAffectedByCycle = emptySet()
        this.retainedRenderOrder = emptyList()
        this.itemCache = mutableMapOf<Key, ItemType>().apply {
            cycleState.stack.current.fastForEach { item ->
                this[resolver.keyFor(item)] = item
            }
        }
        this.observationJob = scope.launch {
            snapshotFlow { stack.value }
                .onStart {
                    // initialize Frame 0 without entrance animation.
                    cycleState.push(
                        stack.value,
                        treatNewEnteringAsPreparing = false,
                        recalculateEnteringToMoving = false
                    )
                }
                .collect { currentStack ->
                    if (lastStackActedUpon != currentStack) {
                        // mount new items in pre entered state and identify affected items
                        startCycle(currentStack)
                        // animate affected items into their settled target positions
                        progressCycle(reuseSettledResolution = true)
                    }
                }
        }
    }

    private val scope: CoroutineScope
    private val stack: State<List<ItemType>>
    private val registry: ItemAnimationRegistry<ItemType, Key, Context>
    private val resolver: ContextResolver<ItemType, Key, Context, CreationContext>
    private val affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context>
    private val maxAffected: Int
    private val renderOrder: RenderOrderStrategy<Key>
    private val cycleState: StackCycleState<ItemType, Key, Context, CreationContext>
    private val externalAnimations: ExternalAnimationRegistry<Key>

    var lastStackActedUpon: List<ItemType>
        private set

    var activeAnimationJobs by mutableStateOf(emptyMap<Key, Job>())
        private set

    private var keysCurrentlyAffectedByCycle: Set<Key>
    private var activeCycleId = 0L
    private var capabilityCycleOpen = false
    private var retainedRenderOrder: List<Key>
    private val itemCache: MutableMap<Key, ItemType>

    var itemsToRender by mutableStateOf(emptyList<Pair<Pair<Key, ItemType>, ItemAnimation<Context>>>())
        private set

    val targetStackKeys get() = cycleState.stack.currentKeysInOrder

    // observes snapshot updates to and drives two-phase transitions
    private val observationJob: Job

    internal fun dispose() {
        observationJob.cancel()
        activeAnimationJobs.values.forEach(Job::cancel)
        activeAnimationJobs = emptyMap()
        itemsToRender = emptyList()
        externalAnimations.clear()
        registry.clear()
        itemCache.clear()
    }

    fun registerExternalAnimation(
        key: Key,
        isRunning: () -> Boolean
    ): ExternalAnimationRegistration = externalAnimations.register(key, isRunning)

    fun startCycle(stackSnapshot: List<ItemType>): Set<Key> =
        startCycle(stackSnapshot, capabilityType = null)

    private fun startCycle(
        stackSnapshot: List<ItemType>,
        capabilityType: KClass<*>?
    ): Set<Key> {
        val stackBeforeCycle = cycleState.stack.current
        capabilityCycleOpen = false
        activeCycleId++
        // prepare new entering items as PreEntered on Frame 0
        cycleState.push(
            stackSnapshot,
            treatNewEnteringAsPreparing = true,
            recalculateEnteringToMoving = false
        )
        stackBeforeCycle.fastForEach { item ->
            itemCache[resolver.keyFor(item)] = item
        }
        cycleState.stack.current.fastForEach { item ->
            itemCache[resolver.keyFor(item)] = item
        }

        lastStackActedUpon = cycleState.stack.current

        val keysParticipatingInThisCycle = buildSet {
            stackBeforeCycle.fastForEach { add(resolver.keyFor(it)) }
            cycleState.stack.current.fastForEach { add(resolver.keyFor(it)) }
        }

        fun animationFor(key: Key): ItemAnimation<Context> =
            registry.getOrCreate(requireNotNull(itemCache[key]), key) {
                cycleState.context.current[key]!!
            }.also {
                if (key in keysParticipatingInThisCycle) {
                    it.selectForCycle(capabilityType)
                }
            }

        // limit active animation work to items within the visible viewport bounds,
        // or whatever other visibility conditions the animations report
        val affectedItems = affectedItemsPolicy.selectAffectedItems(
            cycleState.stack.current,
            keys = cycleState.stack.currentKeyMap,
            isVisible = { key, context -> animationFor(key).willBeVisible(context) },
            currentContexts = cycleState.context.current,
            targetContexts = cycleState.settledContexts(),
            previousContexts = cycleState.context.previous,
            maxAffected = maxAffected,
            minAffected = 2
        )
        affectedItems.forEach(::animationFor)
        val invisibleRemoved = invisibleRemovedItems(
            currentStackKeys = cycleState.stack.currentKeys,
            currentContexts = cycleState.context.current,
            previousContexts = cycleState.context.previous,
            currentAnimations = registry.animations
        )
        val newAffected = affectedItems - invisibleRemoved
        val removedWithoutNewAnimation = buildSet {
            cycleState.context.current.keys.forEach { key ->
                if (key !in cycleState.stack.currentKeys && key !in newAffected) add(key)
            }
        }

        // items removed without any active or required animation
        // can be removed immediately
        val immediatelyEvicted = removedWithoutNewAnimation.filterTo(mutableSetOf()) { key ->
            activeAnimationJobs[key]?.isActive != true
        }

        // cancel previous running jobs for items moving or reentering
        // in this cycle
        val jobsToCancel = mutableListOf<Job>()
        val nextJobs = activeAnimationJobs.toMutableMap().apply {
            immediatelyEvicted.forEach { key ->
                remove(key)?.let(jobsToCancel::add)
            }
            newAffected.forEach { key ->
                put(key, Job())?.let(jobsToCancel::add)
            }
        }
        // publish replacement ownership before cancellation so
        // old jobs won't perform stale eviction in finally
        activeAnimationJobs = nextJobs
        jobsToCancel.fastForEach(Job::cancel)
        immediatelyEvicted.forEach { key ->
            registry.evict(key)
            itemCache -= key
        }
        updateItemsToRender()

        keysCurrentlyAffectedByCycle = newAffected
        return newAffected
    }

    private fun invisibleRemovedItems(
        currentStackKeys: Iterable<Key>,
        currentContexts: Map<Key, Context>,
        previousContexts: Map<Key, Context>,
        currentAnimations: Map<Key, ItemAnimation<Context>>
    ): Set<Key> = buildSet {
        currentContexts.forEach { (key, currentContext) ->
            if (key !in currentStackKeys) {
                val animation = currentAnimations[key]
                val previousContext = previousContexts[key]
                if (animation != null && previousContext != null &&
                    !animation.willBeVisible(previousContext) &&
                    !animation.willBeVisible(currentContext)
                ) {
                    add(key)
                }
            }
        }
    }

    override fun <T : Any> startCycle(
        kClass: KClass<T>
    ): Map<Any, T?> {
        val affected = startCycle(stack.value, capabilityType = kClass)
            .also { capabilityCycleOpen = true }
        return affected.associateWith { affectedItem ->
            registry.animations[affectedItem]?.getAndSelectCapability(kClass)
        }
    }

    override val currentCycleId get() = activeCycleId

    override fun isCycleActive(id: Long) = capabilityCycleOpen && id == activeCycleId

    override fun progressCycle(id: Long) {
        if (!isCycleActive(id)) return
        capabilityCycleOpen = false
        progressCycle()
        activeCycleId++
    }

    override fun progressCycle() = progressCycle(reuseSettledResolution = false)

    private fun progressCycle(reuseSettledResolution: Boolean) {
        val needsStackUpdate = cycleState.stack.current != stack.value
        if (needsStackUpdate) {
            cycleState.push(
                stack.value,
                treatNewEnteringAsPreparing = false,
                recalculateEnteringToMoving = true
            )
        } else {
            if (reuseSettledResolution)
                cycleState.progressUsingSettledResolution()
            else
                cycleState.progress()
        }

        if (needsStackUpdate) updateItemsToRender()

        val currentStackSnapshot = cycleState.stack.current
        val currentContextsSnapshot = cycleState.context.current

        // safe check. only one active animation job per item exists at any time
        fun existsCurrently(key: Key): Boolean = key in cycleState.stack.currentKeys

        lastStackActedUpon = currentStackSnapshot

        val affectedKeysSnapshot = keysCurrentlyAffectedByCycle

        val jobsToCancel = mutableListOf<Job>()
        val jobsToStart = mutableListOf<Job>()
        val nextJobs = activeAnimationJobs.toMutableMap()
        affectedKeysSnapshot.forEach { key ->
            val animation = registry.animations[key]
            val currentContext = currentContextsSnapshot[key]

            if (animation == null || currentContext == null) {
                nextJobs.remove(key)?.let(jobsToCancel::add)
            } else {
                val job = scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        animation.animateTo(currentContext)
                        // wait for any registered external observers, like shared element transitions
                        externalAnimations.awaitIdle(key)
                    } finally {
                        val exists = existsCurrently(key)
                        val ownJob = currentCoroutineContext()[Job]

                        // if a rapid mutation started a newer cycle,
                        // activeAnimationJobs[key] will point to a newer job.
                        // we only evict if this coroutine still has the still
                        // the ownership of the key
                        if (activeAnimationJobs[key] === ownJob) {
                            // keep items in render tree if they declare themselves visible
                            if (!animation.willBeVisible(currentContext) || !exists) {
                                activeAnimationJobs = activeAnimationJobs.toMutableMap().apply {
                                    if (this[key] === ownJob) remove(key)
                                }
                                updateItemsToRender()
                            }

                            if (!exists) {
                                registry.evict(key)
                                itemCache -= key
                            }
                        }
                    }
                }
                nextJobs.put(key, job)?.let(jobsToCancel::add)
                jobsToStart += job
            }
        }

        activeAnimationJobs = nextJobs
        jobsToCancel.fastForEach(Job::cancel)
        jobsToStart.fastForEach(Job::start)

        if (jobsToStart.size != affectedKeysSnapshot.size) {
            updateItemsToRender()
        }
    }

    fun updateItemsToRender() {
        cycleState.stack.current.fastForEach { item ->
            val key = cycleState.stack.currentKeyMap[item]
                ?: error("No key for item during item cache building")
            itemCache[key] = item
        }

        val currentKeysInOrder = cycleState.stack.currentKeysInOrder
        retainedRenderOrder = retainRemovedKeyPositions(
            currentKeys = currentKeysInOrder,
            currentKeySet = cycleState.stack.currentKeys,
            previousOrder = retainedRenderOrder,
            activeKeys = activeAnimationJobs.keys
        )

        val activeKeys = activeAnimationJobs.keys
        val orderedKeys = renderOrder.order(activeKeys, retainedRenderOrder)
        require(orderedKeys.size == activeKeys.size && orderedKeys.toSet() == activeKeys) {
            "RenderOrderStrategy must return every active key exactly once and no other keys."
        }
        itemsToRender = orderedKeys.fastMap { key ->
            val item = itemCache[key]!!
            val animation = registry.animations[key]!!
            (key to item) to animation
        }
    }

    /**
     * Preserves the visual depth and Z-index position of exiting
     * items among the remaining items.
     *
     * Example: If stack [A, B, C] pops B, placing B at the end ([A, C, B])
     * would cause B to pop in front of C during its exit animation.
     * This places B in its original index slot until its removal
     * animation completes
     */
    private fun retainRemovedKeyPositions(
        currentKeys: List<Key>,
        currentKeySet: Set<Key>,
        previousOrder: List<Key>,
        activeKeys: Set<Key>
    ): List<Key> {
        if (previousOrder == currentKeys) return currentKeys

        val retained = previousOrder.fastFilter { it !in currentKeySet && it in activeKeys }
        if (retained.isEmpty()) return currentKeys

        val result = MutableList<Key?>(currentKeys.size + retained.size) { null }
        val lastIndex = result.lastIndex
        val overflow = mutableListOf<Key>()
        val previousIndices = HashMap<Key, Int>(previousOrder.size)
        previousOrder.fastForEachIndexed { index, key -> previousIndices[key] = index }
        retained.fastForEach { key ->
            val previousIndex = previousIndices.getValue(key)
            if (previousIndex <= lastIndex && result[previousIndex] == null) {
                result[previousIndex] = key
            } else {
                overflow += key
            }
        }

        var availableFromEnd = lastIndex
        overflow.fastForEach { key ->
            while (result[availableFromEnd] != null) availableFromEnd--
            result[availableFromEnd] = key
            availableFromEnd--
        }

        val currentIterator = currentKeys.iterator()
        return result.fastMap { it ?: currentIterator.next() }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <Context> ItemAnimation<Context>.selectForCycle(capabilityType: KClass<*>?) {
    val selectable = this as? SelectableItemAnimation<Context> ?: return
    if (capabilityType == null) {
        selectable.selectDefaultAnimation()
    } else {
        selectable.selectAnimationForCapability(capabilityType)
    }
}
