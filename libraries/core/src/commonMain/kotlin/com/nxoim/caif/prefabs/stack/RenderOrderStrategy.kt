package com.nxoim.caif.prefabs.stack

import androidx.collection.MutableScatterSet
import androidx.compose.ui.util.fastForEach

/**
 * Orders active keys from back to front for rendering and must return every active key exactly
 * once.
 */
fun interface RenderOrderStrategy<Key : Any> {
    fun order(activeKeys: Set<Key>, stackOrderKeys: List<Key>): List<Key>

    companion object {
        private val InsertionStrategy = RenderOrderStrategy<Any> { active, _ -> active.toList() }

        private val TopmostFirstStackIndexStrategy =
            createStackIndexStrategy(StackOrder.TopmostFirst)
        private val TopmostLastStackIndexStrategy = createStackIndexStrategy(StackOrder.TopmostLast)

        private fun createStackIndexStrategy(order: StackOrder) =
            RenderOrderStrategy<Any> { active, stackOrder ->
                val remaining = MutableScatterSet<Any>(active.size).apply {
                    active.forEach { add(it) }
                }
                buildList(active.size) {
                    val backToFront = when (order) {
                        StackOrder.TopmostFirst -> stackOrder.asReversed()
                        StackOrder.TopmostLast -> stackOrder
                    }
                    backToFront.fastForEach { key ->
                        if (remaining.remove(key)) add(key)
                    }
                    for (key in active) {
                        if (remaining.remove(key)) add(key)
                    }
                }
            }

        @Suppress("UNCHECKED_CAST")
        fun <Key : Any> insertionOrder(): RenderOrderStrategy<Key> =
            InsertionStrategy as RenderOrderStrategy<Key>

        /**
         * Renders the supplied [stackOrder] from bottom to top.
         */
        @Suppress("UNCHECKED_CAST")
        fun <Key : Any> byStackIndex(
            stackOrder: StackOrder = StackOrder.TopmostLast
        ): RenderOrderStrategy<Key> = when (stackOrder) {
            StackOrder.TopmostFirst -> TopmostFirstStackIndexStrategy
            StackOrder.TopmostLast -> TopmostLastStackIndexStrategy
        } as RenderOrderStrategy<Key>
    }
}
