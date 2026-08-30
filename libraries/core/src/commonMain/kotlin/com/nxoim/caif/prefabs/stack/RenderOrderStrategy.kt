package com.nxoim.caif.prefabs.stack

import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.ui.util.fastForEach

/**
 * Orders active keys from back to front for rendering and must return every active key exactly
 * once.
 */
fun interface RenderOrderStrategy<Key : Any> {
    fun order(activeKeys: Set<Key>, stackOrderKeys: List<Key>): List<Key>

    companion object {
        private val InsertionStrategy = RenderOrderStrategy<Any> { active, _ -> active.toList() }

        private val ByStackIndexStrategyInstance = RenderOrderStrategy<Any> { active, stackOrder ->
            val remaining = active.toMutableSet()
            buildList(active.size) {
                stackOrder.asReversed().fastForEach { key ->
                    if (remaining.remove(key)) add(key)
                }
                for (key in active) {
                    if (remaining.remove(key)) add(key)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        @RememberInComposition
        fun <Key : Any> insertionOrder(): RenderOrderStrategy<Key> =
            InsertionStrategy as RenderOrderStrategy<Key>

        @Suppress("UNCHECKED_CAST")
        @RememberInComposition
        fun <Key : Any> byStackIndex(): RenderOrderStrategy<Key> =
            ByStackIndexStrategyInstance as RenderOrderStrategy<Key>
    }
}
