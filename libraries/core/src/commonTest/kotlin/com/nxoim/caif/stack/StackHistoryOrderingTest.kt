package com.nxoim.caif.stack

import com.nxoim.caif.prefabs.stack.ContextHistory
import com.nxoim.caif.prefabs.stack.ContextResolver
import com.nxoim.caif.prefabs.stack.StackHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StackHistoryOrderingTest {

    data class StackItem(val id: String, val title: String)

    @Test
    fun givenStackHistory_whenPushingOrderedItems_thenPreservesExactStackOrder() {
        val history = StackHistory<StackItem, String> { it.id }

        val items = listOf(
            StackItem("item-1", "First"),
            StackItem("item-2", "Second"),
            StackItem("item-3", "Third")
        )

        history.push(items)

        assertEquals(listOf("item-1", "item-2", "item-3"), history.currentKeysInOrder)
        assertEquals(setOf("item-1", "item-2", "item-3"), history.currentKeys)
        assertEquals("item-1", history.currentKeyMap[items[0]])
        assertEquals("item-2", history.currentKeyMap[items[1]])
        assertEquals("item-3", history.currentKeyMap[items[2]])
        assertEquals(emptyList<StackItem>(), history.previous)
        assertEquals(items, history.current)

        val nextItems = listOf(
            StackItem("item-1", "First"),
            StackItem("item-3", "Third"),
            StackItem("item-4", "Fourth")
        )
        history.push(nextItems)

        assertEquals(listOf("item-1", "item-3", "item-4"), history.currentKeysInOrder)
        assertEquals(items, history.previous)
        assertEquals(nextItems, history.current)
    }

    @Test
    fun givenStackHistory_whenDuplicateItemPresent_thenFailsWithRequirement() {
        val history = StackHistory<StackItem, String> { it.id }

        val duplicateItems = listOf(
            StackItem("item-1", "First"),
            StackItem("item-1", "First")
        )

        assertFailsWith<IllegalArgumentException> {
            history.push(duplicateItems)
        }
    }

    @Test
    fun givenStackHistory_whenDuplicateKeysPresentForDifferentItems_thenFailsWithRequirement() {
        val history = StackHistory<StackItem, String> { it.id }

        val collidingKeys = listOf(
            StackItem("same-key", "Item A"),
            StackItem("same-key", "Item B")
        )

        assertFailsWith<IllegalArgumentException> {
            history.push(collidingKeys)
        }
    }

    @Test
    fun givenContextHistory_whenPushingResolutions_thenMaintainsSnapshotImmutability() {
        val resolver = object : ContextResolver<String, String, Int, String> {
            override fun keyFor(itemType: String): String = itemType
            override fun buildContexts(
                stack: List<String>,
                previousStack: List<String>?,
                treatNewEnteringAsPreparing: Boolean,
                recalculateEnteringToMoving: Boolean,
                previousContexts: Map<String, Int>?
            ): Map<String, Pair<Int, String>> =
                stack.associateWith { (it.length) to "Creation-$it" }
        }

        val history = ContextHistory(resolver)

        history.pushResolved(
            mapOf("A" to (1 to "ctx-A"), "B" to (2 to "ctx-B")),
            preserveUnresolvedContexts = false
        )

        assertEquals(mapOf("A" to 1, "B" to 2), history.current)

        history.pushResolved(
            mapOf("B" to (20 to "ctx-B2"), "C" to (30 to "ctx-C")),
            preserveUnresolvedContexts = true
        )

        assertEquals(mapOf("B" to 20, "C" to 30, "A" to 1), history.current)
        assertEquals(mapOf("A" to 1, "B" to 2), history.previous)
    }
}
