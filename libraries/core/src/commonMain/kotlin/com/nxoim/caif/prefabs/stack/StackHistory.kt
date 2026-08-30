package com.nxoim.caif.prefabs.stack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastForEach

class StackHistory<ItemType, Key : Any>(
    private val keyFor: (ItemType) -> Key
) {
    private var state by mutableStateOf(
        StackHistoryState<ItemType, Key>(
            previous = null,
            current = emptyList(),
            currentKeyMap = emptyMap(),
            currentKeys = emptySet(),
            currentKeysInOrder = emptyList()
        )
    )

    val previous get() = state.previous
    val current get() = state.current
    val currentKeyMap get() = state.currentKeyMap
    internal val currentKeys get() = state.currentKeys
    internal val currentKeysInOrder get() = state.currentKeysInOrder

    fun push(newStack: List<ItemType>) {
        val previousStack = state.current
        if (previousStack == newStack) return

        if (newStack.isEmpty()) {
            state = StackHistoryState(
                previous = previousStack,
                current = newStack,
                currentKeyMap = emptyMap(),
                currentKeys = emptySet(),
                currentKeysInOrder = emptyList()
            )
            return
        }

        val currentKeyMap = LinkedHashMap<ItemType, Key>(newStack.size)
        // the set keeps membership O(1), while the list preserves stack order
        val currentKeys = HashSet<Key>(newStack.size)
        val currentKeysInOrder = ArrayList<Key>(newStack.size)

        newStack.fastForEach { item ->
            val key = keyFor(item)
            val previousKey = currentKeyMap.put(item, key)
            require(previousKey == null) {
                "A stack snapshot contains equal items. ContextResolver receives keys as a Map<ItemType, Key>, " +
                        "so equal items cannot be represented independently even when keyFor returns different keys."
            }

            require(currentKeys.add(key)) {
                "keyFor produced the same key for two different items in one stack snapshot: $key. " +
                        "Keys must uniquely identify items within a single stack — if two different items can " +
                        "legitimately share whatever keyFor currently returns, keyFor needs to derive a more " +
                        "specific key (e.g. include a distinguishing field), not just the item's display data."
            }

            currentKeysInOrder += key
        }

        state = StackHistoryState(
            previous = previousStack,
            current = newStack,
            currentKeyMap = currentKeyMap,
            currentKeys = currentKeys,
            currentKeysInOrder = currentKeysInOrder
        )
    }
}

private class StackHistoryState<ItemType, Key : Any>(
    val previous: List<ItemType>?,
    val current: List<ItemType>,
    val currentKeyMap: Map<ItemType, Key>,
    val currentKeys: Set<Key>,
    val currentKeysInOrder: List<Key>
)

class ContextHistory<ItemType, Key : Any, Context, CreationContext>(
    private val resolver: ContextResolver<ItemType, Key, Context, CreationContext>
) {
    private var state by mutableStateOf(
        ContextHistoryState<Key, Context, CreationContext>(
            previous = emptyMap(),
            current = emptyMap(),
            creationContexts = emptyMap()
        )
    )

    val previous get() = state.previous
    val current get() = state.current

    fun push(
        newStack: List<ItemType>,
        previousStack: List<ItemType>?,
        treatNewEnteringAsPreparing: Boolean,
        recalculateEnteringToMoving: Boolean,
        preserveUnresolvedContexts: Boolean = false
    ) {
        val resolvedContexts = resolver.buildContexts(
            newStack,
            previousStack,
            treatNewEnteringAsPreparing,
            recalculateEnteringToMoving,
            state.current
        )

        pushResolved(resolvedContexts, preserveUnresolvedContexts)
    }

    internal fun pushResolved(
        resolvedContexts: Map<Key, Pair<Context, CreationContext>>,
        preserveUnresolvedContexts: Boolean = false
    ) {
        val previousState = state
        val previousContexts = previousState.current

        // a second context resolution for the same stack
        // must not discard that item before the orchestrator can settle it
        if (preserveUnresolvedContexts && resolvedContexts.isEmpty()) {
            state = ContextHistoryState(
                previous = previousContexts,
                current = previousContexts,
                creationContexts = previousState.creationContexts
            )
            return
        }

        if (resolvedContexts.isEmpty()) {
            state = ContextHistoryState(
                previous = previousContexts,
                current = emptyMap(),
                creationContexts = emptyMap()
            )
            return
        }

        val expectedSize = if (preserveUnresolvedContexts) {
            maxOf(resolvedContexts.size, previousContexts.size)
        } else {
            resolvedContexts.size
        }
        val currentContexts = LinkedHashMap<Key, Context>(expectedSize)
        val creationContexts = LinkedHashMap<Key, CreationContext>(expectedSize)
        resolvedContexts.forEach { (key, resolution) ->
            currentContexts[key] = resolution.first
            creationContexts[key] = resolution.second
        }

        if (preserveUnresolvedContexts) {
            previousContexts.forEach { (key, context) ->
                if (key !in resolvedContexts) {
                    currentContexts[key] = context
                    creationContexts[key] = previousState.creationContexts[key]
                        ?: error("Missing creation context")
                }
            }
        }

        state = ContextHistoryState(
            previous = previousContexts,
            current = currentContexts,
            creationContexts = creationContexts
        )
    }
}

private class ContextHistoryState<Key : Any, Context, CreationContext>(
    val previous: Map<Key, Context>,
    val current: Map<Key, Context>,
    val creationContexts: Map<Key, CreationContext>
)

/**
 * Snapshot history and coordination of the two-phase context transition for a given cycle
 */
class StackCycleState<ItemType, Key : Any, Context, CreationContext>(
    private val resolver: ContextResolver<ItemType, Key, Context, CreationContext>,
) {
    val stack = StackHistory(resolver::keyFor)
    val context = ContextHistory(resolver)

    private var previousStackForCurrentCycle: List<ItemType>? = null
    private var settledResolution: Map<Key, Pair<Context, CreationContext>>? = null

    fun push(
        newStack: List<ItemType>,
        treatNewEnteringAsPreparing: Boolean,
        recalculateEnteringToMoving: Boolean
    ) {
        val previousStack = stack.current
        val needsStackUpdate = previousStack != newStack

        if (needsStackUpdate) {
            stack.push(newStack)
        }

        previousStackForCurrentCycle = if (needsStackUpdate) previousStack else newStack
        settledResolution = null

        context.push(
            newStack,
            previousStackForCurrentCycle,
            treatNewEnteringAsPreparing,
            recalculateEnteringToMoving
        )
    }

    /**
     * Resolves the prepared state into its settled state without changing the
     * stack snapshot. This is needed after startCycle has prepared a newly
     * entered item as PreEntered: the subsequent progress pass must target its
     * actual stack position.
     */
    fun progress() = progress(reuseSettledResolution = false)

    internal fun progressUsingSettledResolution() = progress(reuseSettledResolution = true)

    private fun progress(reuseSettledResolution: Boolean) {
        val resolution = if (reuseSettledResolution) {
            settledResolution ?: buildSettledResolution()
        } else {
            buildSettledResolution()
        }
        context.pushResolved(
            resolvedContexts = resolution,
            preserveUnresolvedContexts = true
        )
        settledResolution = null
    }

    /**
     * Lazily evaluates and caches the final settled contexts for the current cycle.
     * Used by [AffectedItemsPolicy] to determine which items need spring animations.
     */
    fun settledContexts(): Map<Key, Context> {
        val resolution = settledResolution ?: buildSettledResolution().also {
            settledResolution = it
        }
        return resolution.mapValues { it.value.first }
    }

    private fun buildSettledResolution() = resolver.buildContexts(
        stack = stack.current,
        previousStack = previousStackForCurrentCycle,
        treatNewEnteringAsPreparing = false,
        recalculateEnteringToMoving = true,
        previousContexts = context.current
    )
}
