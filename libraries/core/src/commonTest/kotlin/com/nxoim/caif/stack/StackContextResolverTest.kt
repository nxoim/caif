package com.nxoim.caif.stack

import com.nxoim.caif.prefabs.stack.StackCreationContext
import com.nxoim.caif.prefabs.stack.defaultStackContextResolver
import com.nxoim.caif.prefabs.stack.indexOf
import com.nxoim.caif.prefabs.stack.previousIndexOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StackContextResolverTest {
    @Test
    fun givenMissingPreviousSnapshot_whenResolving_thenPreviousIndicesAreNull() {
        val resolved = resolve(listOf("a"), previous = null)
        val (indices, creation) = resolved.getValue("a")

        assertEquals(Indices(0, null), indices)
        assertEquals(-1, creation.indexOf("absent"))
        assertNull(creation.previousIndexOf("absent"))
    }

    @Test
    fun givenChangedStack_whenResolving_thenAddedAndRemovedItemsKeepIndexOfSemantics() {
        val resolved = resolve(listOf("kept", "added"), previous = listOf("removed", "kept"))

        assertEquals(Indices(-1, 0), resolved.getValue("removed").first)
        assertEquals(Indices(0, 1), resolved.getValue("kept").first)
        assertEquals(Indices(1, null), resolved.getValue("added").first)
        assertNull(resolved.getValue("kept").second.previousIndexOf("absent"))
    }

    @Test
    fun givenStandaloneResolverWithDuplicates_whenResolving_thenUsesFirstEqualItemIndex() {
        // StackHistory rejects duplicate keys, but the standalone resolver also accepts lists directly.
        val resolved = resolve(
            listOf("duplicate", "other", "duplicate"),
            previous = listOf("other", "duplicate", "duplicate")
        )

        assertEquals(Indices(0, 1), resolved.getValue("duplicate").first)
        assertEquals(Indices(1, 0), resolved.getValue("other").first)
    }

    @Test
    fun givenNullableItems_whenResolving_thenNullHasItsOwnIndices() {
        val resolved = resolve(listOf(null, "a"), previous = listOf("a", null))

        assertEquals(Indices(0, 1), resolved.getValue("<null>").first)
        assertEquals(Indices(1, 0), resolved.getValue("a").first)
    }

    @Test
    fun givenResolvedCreationContext_whenCopyingWithDifferentSnapshots_thenUsesCopiedSnapshots() {
        val original = resolve(listOf("a", "b"), previous = null).getValue("a").second
        val copy = original.copy(stackSnapshot = listOf("b", "a"), previousSnapshot = listOf("a"))

        assertEquals(0, original.indexOf("a"))
        assertNull(original.previousIndexOf("a"))
        assertEquals(1, copy.indexOf("a"))
        assertEquals(0, copy.indexOf("b"))
        assertEquals(0, copy.previousIndexOf("a"))
        assertNull(copy.previousIndexOf("b"))
    }

    private data class Indices(val current: Int, val previous: Int?)

    private fun resolve(
        stack: List<String?>,
        previous: List<String?>?
    ): Map<String, Pair<Indices, StackCreationContext<String?>>> =
        defaultStackContextResolver<Indices, String?, String>(
            contextFactory = { item -> Indices(indexOf(item), previousIndexOf(item)) },
            keyFor = { it ?: "<null>" }
        ).buildContexts(
            stack = stack,
            previousStack = previous,
            treatNewEnteringAsPreparing = false,
            recalculateEnteringToMoving = true,
            previousContexts = null
        )
}
