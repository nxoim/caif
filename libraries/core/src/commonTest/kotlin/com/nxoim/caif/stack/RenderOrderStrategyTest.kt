package com.nxoim.caif.stack

import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderOrderStrategyTest {

    @Test
    fun givenByStackIndex_whenAllItemsActive_thenOrdersFromReversedStackOrder() {
        val strategy = RenderOrderStrategy.byStackIndex<String>()
        val stackOrder = listOf("bottom", "middle", "top")
        val active = setOf("bottom", "middle", "top")

        val ordered = strategy.order(active, stackOrder)
        assertEquals(listOf("top", "middle", "bottom"), ordered)
    }

    @Test
    fun givenByStackIndex_whenExitingItemNotInStack_thenPlacesExitingItemAtEnd() {
        val strategy = RenderOrderStrategy.byStackIndex<String>()
        val stackOrder = listOf("bottom", "top") // "middle" was popped from stack
        val active = setOf("bottom", "middle", "top") // "middle" is still playing exit animation

        val ordered = strategy.order(active, stackOrder)
        assertEquals(listOf("top", "bottom", "middle"), ordered)
    }

    @Test
    fun givenByStackIndex_whenSubsetOfStackIsActive_thenOnlyReturnsActiveKeysInReversedOrder() {
        val strategy = RenderOrderStrategy.byStackIndex<String>()
        val stackOrder = listOf("A", "B", "C", "D", "E")
        val active = setOf("B", "D")

        val ordered = strategy.order(active, stackOrder)
        assertEquals(listOf("D", "B"), ordered)
    }

    @Test
    fun givenInsertionOrder_whenGivenActiveKeys_thenPreservesActiveKeysOrder() {
        val strategy = RenderOrderStrategy.insertionOrder<String>()
        val active = linkedSetOf("first", "second", "third")
        val stackOrder = listOf("third", "first")

        val ordered = strategy.order(active, stackOrder)
        assertEquals(listOf("first", "second", "third"), ordered)
    }
}
