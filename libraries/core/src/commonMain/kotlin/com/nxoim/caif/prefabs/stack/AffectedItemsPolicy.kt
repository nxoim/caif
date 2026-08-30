package com.nxoim.caif.prefabs.stack

import androidx.collection.mutableOrderedScatterSetOf

/** Selects the stack keys that participate in the next animation cycle. */
fun interface AffectedItemsPolicy<ItemType, Key : Any, Context> {
    fun selectAffectedItems(
        stack: List<ItemType>,
        keys: Map<ItemType, Key>,
        isVisible: (Key, Context) -> Boolean,
        currentContexts: Map<Key, Context>,
        targetContexts: Map<Key, Context>,
        previousContexts: Map<Key, Context>,
        maxAffected: Int,
        minAffected: Int
    ): Set<Key>

    companion object {
        /**
         * Selects affected items starting at the top of the stack. By default, the last item in
         * stack is treated as the topmost item; set [reversed] to `false` when the first item is
         * the topmost item instead.
         */
        @Suppress("UNCHECKED_CAST")
        fun <ItemType, Key : Any, Context> fromTop(
            reversed: Boolean = true
        ) = (if (reversed) FromTopReversed else FromTopForward) as AffectedItemsPolicy<ItemType, Key, Context>
    }
}

// bounds animation work on deep stacks by only activating items
// at the top of the stack that are either currently visible or
// transitioning from/to visibility
private class CreateFromTopPolicy(private val reversed: Boolean) : AffectedItemsPolicy<Any?, Any, Any?> {
    override fun selectAffectedItems(
        stack: List<Any?>,
        keys: Map<Any?, Any>,
        isVisible: (Any, Any?) -> Boolean,
        currentContexts: Map<Any, Any?>,
        targetContexts: Map<Any, Any?>,
        previousContexts: Map<Any, Any?>,
        maxAffected: Int,
        minAffected: Int
    ): Set<Any> {
        val total = mutableOrderedScatterSetOf<Any>()
        val stackIndices = stack.indices.let { if (reversed) it.reversed() else it }

        for (index in stackIndices) {
            if (total.size >= maxAffected) break

            val item = stack[index]
            val key = keys[item] ?: continue
            val targetContext = targetContexts[key] ?: continue
            val willBeVisible = isVisible(key, targetContext)
            val wasVisible = previousContexts[key]
                ?.let { isVisible(key, it) }
                ?: false
            val transitioningToInvisible = wasVisible && !willBeVisible
            val minimumNotReached = total.size < minAffected

            // once we encounter occluded items that are neither
            // visible nor transitioning we can safely stop traversing
            // the stack
            if (!minimumNotReached && !willBeVisible && !transitioningToInvisible) {
                break
            }

            total.add(key)
        }

        for ((key, _) in currentContexts) {
            if (total.size >= maxAffected) break
            if (key !in keys.values) {
                val wasVisible = previousContexts[key]
                    ?.let { isVisible(key, it) }
                    ?: false
                if (wasVisible) total.add(key)
            }
        }

        return total.asSet()
    }
}

private val FromTopReversed = CreateFromTopPolicy(reversed = true)
private val FromTopForward = CreateFromTopPolicy(reversed = false)