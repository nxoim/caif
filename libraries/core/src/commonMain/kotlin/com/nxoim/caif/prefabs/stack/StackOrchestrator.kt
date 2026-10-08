@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.caif.prefabs.stack

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.toMutableScatterMap
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import com.nxoim.caif.core.DeclaredItemAnimation
import com.nxoim.caif.core.ItemAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

class StackOrchestrator<ItemType, Key : Any, Context, CreationContext>(
    private val scope: CoroutineScope,
    private val stack: State<List<ItemType>>,
    private val registry: ItemAnimationRegistry<ItemType, Key, Context>,
    private val resolver: ContextResolver<ItemType, Key, Context, CreationContext>,
    private val affectedItemsPolicy: AffectedItemsPolicy<ItemType, Key, Context> = AffectedItemsPolicy.fromTop(),
    private val maxAffected: Int,
    private val renderOrder: RenderOrderStrategy<Key>
) : StackCycleController {
    init {
        require(maxAffected >= 0) { "maxAffected must not be negative." }
    }

    private val cycleState = StackCycleState(resolver)
    private val externalAnimations = ExternalAnimationRegistry<Key>()

    var lastStackActedUpon = emptyList<ItemType>()
        private set

    // entries retain rendered items, including visible items whose
    // jobs have completed. insertion order is observed by
    // RenderOrderStrategy.insertionOrder().
    private val renderJobs = mutableMapOf<Key, Job>()

    private var keysCurrentlyAffectedByCycle = emptySet<Key>()
    private var activeCycleId = 0L
    private var capabilityCycleOpen = false
    private var retainedRenderOrder = emptyList<Key>()
    private val itemCache = mutableScatterMapOf<Key, ItemType>()

    var itemsToRender by mutableStateOf(emptyList<Pair<Pair<Key, ItemType>, ItemAnimation<Context>>>())
        private set

    val targetStackKeys get() = cycleState.stack.currentKeysInOrder

    // observes snapshot updates to and drives two-phase transitions
    private val observationJob = scope.launch {
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

    override val currentCycleId get() = activeCycleId

    fun registerExternalAnimation(
        key: Key,
        isRunning: () -> Boolean
    ): ExternalAnimationRegistration = externalAnimations.register(key, isRunning)

    fun startCycle(stackSnapshot: List<ItemType>): Set<Key> =
        startCycle(stackSnapshot, capabilityType = null)

    override fun <T : Any> startCycle(
        kClass: KClass<T>
    ): Map<Any, T?> {
        val affected = startCycle(stack.value, capabilityType = kClass)
            .also { capabilityCycleOpen = true }

        return buildMap(affected.size) {
            affected.forEach { affectedItem ->
                put(affectedItem, registry.get(affectedItem)?.getAndSelectCapability(kClass))
            }
        }
    }

    override fun isCycleActive(id: Long) = capabilityCycleOpen && id == activeCycleId

    override fun progressCycle(id: Long) {
        if (!isCycleActive(id)) return
        capabilityCycleOpen = false
        progressCycle()
        activeCycleId++
    }

    override fun progressCycle() = progressCycle(reuseSettledResolution = false)

    fun updateItemsToRender() {
        val currentKeysInOrder = cycleState.stack.currentKeysInOrder
        val activeKeys = renderJobs.keys
        retainedRenderOrder = retainRemovedKeyPositions(
            currentKeys = currentKeysInOrder,
            currentKeySet = cycleState.stack.currentKeys,
            previousOrder = retainedRenderOrder,
            activeKeys = activeKeys
        )

        val orderedKeys = renderOrder.order(activeKeys, retainedRenderOrder)
        require(orderedKeys.containsExactly(activeKeys)) {
            "RenderOrderStrategy must return every active key exactly once and no other keys."
        }
        itemsToRender = orderedKeys.fastMap { key ->
            val item = itemCache[key]!!
            val animation = registry.get(key)!!
            (key to item) to animation
        }
    }

    internal fun dispose() {
        observationJob.cancel()
        // cancellation can synchronously resume jobs that remove
        // themselves from this map
        renderJobs.toMutableScatterMap().forEachValue(Job::cancel)
        renderJobs.clear()
        itemsToRender = emptyList()
        externalAnimations.clear()
        registry.clear()
        itemCache.clear()
    }

    private fun startCycle(
        stackSnapshot: List<ItemType>,
        capabilityType: KClass<*>?
    ): Set<Key> {
        registry.beginCycle()
        val keysBeforeCycle = cycleState.stack.currentKeys
        val itemsAndKeysBeforeCycle = cycleState.stack.currentKeyMap
        capabilityCycleOpen = false
        activeCycleId++
        // prepare new entering items as PreEntered on Frame 0
        cycleState.push(
            stackSnapshot,
            treatNewEnteringAsPreparing = true,
            recalculateEnteringToMoving = false
        )
        cacheItems(itemsAndKeysBeforeCycle)
        val currentStack = cycleState.stack.current
        val currentItemsAndKeys = cycleState.stack.currentKeyMap
        val currentKeys = cycleState.stack.currentKeys
        cacheItems(currentItemsAndKeys)

        lastStackActedUpon = cycleState.stack.current

        val sharedDefinitions = registry.sourceItem(capabilityType, currentStack)?.let { source ->
            val sourceKey = currentItemsAndKeys[source]
            require(sourceKey != null) {
                "The animation selection source must belong to the target stack snapshot."
            }
            val animation = registry.getOrCreateDeclared(source, sourceKey) {
                cycleState.context.current.getValue(sourceKey)
            }
            require(animation.definitions.supportsSharedSelection) {
                "Shared selection requires a factory built with buildAnimationFactory."
            }
            animation.definitions
        }
        val sharedDefinition = sharedDefinitions?.forInput(capabilityType)

        fun animationFor(key: Key): DeclaredItemAnimation<Context> =
            registry.getOrCreateDeclared(requireNotNull(itemCache[key]), key) {
                cycleState.context.current[key]!!
            }.also {
                if (key in keysBeforeCycle || key in currentKeys) {
                    it.select(
                        sharedDefinition ?: it.definitionFor(capabilityType),
                        sharedDefinitions ?: it.definitions,
                    )
                }
            }

        renderJobs.keys.forEach { key ->
            if (key in keysBeforeCycle || key in currentKeys) animationFor(key)
        }

        val settledContexts = cycleState.settledContexts()
        // limit active animation work to items within the visible viewport bounds,
        // or whatever other visibility conditions the animations report
        val affectedItems = affectedItemsPolicy.selectAffectedItems(
            cycleState.stack.current,
            keys = cycleState.stack.currentKeyMap,
            isVisible = { key, context -> animationFor(key).willBeVisible(context) },
            currentContexts = cycleState.context.current,
            targetContexts = settledContexts,
            previousContexts = cycleState.context.previous,
            maxAffected = maxAffected,
            minAffected = 2
        )
        affectedItems.forEach(::animationFor)
        val newAffected = LinkedHashSet(affectedItems)
        val previousContexts = cycleState.context.previous
        cycleState.context.current.forEach { (key, currentContext) ->
            if (key !in currentKeys) {
                val animation = registry.get(key)
                val previousContext = previousContexts[key]
                if (animation != null && previousContext != null &&
                    !animation.willBeVisible(previousContext) && !animation.willBeVisible(currentContext)
                ) {
                    newAffected.remove(key)
                }
            }
        }

        // items removed without any active or required animation
        // can be removed immediately
        val immediatelyEvicted = MutableScatterSet<Key>().apply {
            cycleState.context.current.keys.forEach { key ->
                if (key !in currentKeys && key !in newAffected && renderJobs[key]?.isActive != true) add(key)
            }
        }

        // cancel previous running jobs for items moving or reentering
        // in this cycle
        val jobsToCancel = mutableListOf<Job>()
        immediatelyEvicted.forEach { key ->
            renderJobs.remove(key)?.let(jobsToCancel::add)
        }
        newAffected.forEach { key ->
            renderJobs.put(key, Job())?.let(jobsToCancel::add)
        }
        jobsToCancel.fastForEach(Job::cancel)
        immediatelyEvicted.forEach { key ->
            registry.evict(key)
            itemCache -= key
        }
        val hiddenCompletedKeys = renderJobs.filter { (key, job) ->
            key in cycleState.stack.currentKeys && key !in newAffected && !job.isActive &&
                settledContexts[key]?.let { registry.get(key)?.willBeVisible(it) == false } == true
        }.keys

        hiddenCompletedKeys.forEach { key ->
            if (externalAnimations.isRunning(key)) {
                // a shared transition may still retain the view
                val job = createRenderJob(key)
                renderJobs[key] = job
                job.start()
            } else {
                renderJobs.remove(key)
            }
        }
        updateItemsToRender()

        keysCurrentlyAffectedByCycle = newAffected
        return newAffected
    }

    private fun progressCycle(reuseSettledResolution: Boolean) {
        val needsStackUpdate = cycleState.stack.current != stack.value
        if (needsStackUpdate) {
            cycleState.push(
                stack.value,
                treatNewEnteringAsPreparing = false,
                recalculateEnteringToMoving = true
            )
            cacheItems(cycleState.stack.currentKeyMap)
        } else if (reuseSettledResolution) {
            cycleState.progressUsingSettledResolution()
        } else {
            cycleState.progress()
        }

        if (needsStackUpdate) updateItemsToRender()

        val currentStackSnapshot = cycleState.stack.current
        val currentContextsSnapshot = cycleState.context.current

        lastStackActedUpon = currentStackSnapshot

        val affectedKeysSnapshot = keysCurrentlyAffectedByCycle

        val jobsToCancel = mutableListOf<Job>()
        val jobsToStart = mutableListOf<Job>()
        affectedKeysSnapshot.forEach { key ->
            val animation = registry.get(key)
            val currentContext = currentContextsSnapshot[key]

            if (animation == null || currentContext == null) {
                renderJobs.remove(key)?.let(jobsToCancel::add)
            } else {
                val job = createRenderJob(key) { animation.animateTo(currentContext) }
                renderJobs.put(key, job)?.let(jobsToCancel::add)
                jobsToStart += job
            }
        }

        jobsToCancel.fastForEach(Job::cancel)
        jobsToStart.fastForEach(Job::start)

        if (jobsToStart.size != affectedKeysSnapshot.size) {
            updateItemsToRender()
        }
    }

    private fun cacheItems(itemsAndKeys: Map<ItemType, Key>) {
        itemsAndKeys.forEach { (item, key) -> itemCache[key] = item }
    }

    private fun createRenderJob(key: Key, animate: suspend () -> Unit = {}): Job =
        // jobs start after registration so cleanup can check ownership
        scope.launch(start = CoroutineStart.LAZY) {
            try {
                animate()
                externalAnimations.awaitIdle(key)
            } finally {
                finishRendering(key)
            }
        }

    private suspend fun finishRendering(key: Key) {
        // cleanup belongs to the job currently registered for this key.
        if (renderJobs[key] !== currentCoroutineContext()[Job]) return
        val exists = key in cycleState.stack.currentKeys
        val context = cycleState.context.current[key]

        if (!exists || context == null || registry.get(key)?.willBeVisible(context) == false) {
            renderJobs.remove(key)
            updateItemsToRender()
        }

        if (!exists) {
            registry.evict(key)
            itemCache -= key
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
        previousOrder.fastForEachIndexed { previousIndex, key ->
            if (key in currentKeySet || key !in activeKeys) return@fastForEachIndexed
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

private fun <Key : Any> List<Key>.containsExactly(activeKeys: Set<Key>): Boolean {
    if (size != activeKeys.size) return false

    val remainingKeys = MutableScatterSet<Key>(activeKeys.size).apply {
        activeKeys.forEach(::add)
    }
    fastForEach { key ->
        if (!remainingKeys.remove(key)) return false
    }
    return remainingKeys.size == 0
}
