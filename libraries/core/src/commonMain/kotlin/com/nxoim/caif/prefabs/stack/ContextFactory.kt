package com.nxoim.caif.prefabs.stack

import androidx.compose.runtime.Immutable

@Immutable
fun interface ContextFactory<ItemType, Context, CreationContext> {
    operator fun CreationContext.invoke(item: ItemType): Context

    fun create(
        context: CreationContext,
        item: ItemType
    ): Context = context.invoke(item)
}

/**
 * Snapshot lists are retained by reference and must not be
 * mutated while the context is in use.
 */
@Immutable
data class StackCreationContext<ItemType>(
    val stackSnapshot: List<ItemType>,
    val previousSnapshot: List<ItemType>? = null,
    val intention: AppearanceIntention,
) {
    // keep the cache outside the primary constructor to
    // preserve data class equality and copy
    internal var indexLookup: StackCreationContextIndexLookup<ItemType>? = null
        private set

    internal fun installIndexLookup(lookup: StackCreationContextIndexLookup<ItemType>) {
        check(indexLookup == null) { "Stack creation context index lookup can only be installed once." }
        indexLookup = lookup
    }
}

internal class StackCreationContextIndexLookup<ItemType>(
    private val stackSnapshot: List<ItemType>,
    private val previousSnapshot: List<ItemType>?
) {
    private val stackIndices by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildIndices(stackSnapshot)
    }
    private val previousIndices by lazy(LazyThreadSafetyMode.PUBLICATION) {
        previousSnapshot?.let(::buildIndices)
    }

    fun indexOf(item: ItemType): Int = stackIndices[item] ?: -1

    fun previousIndexOf(item: ItemType): Int? = previousIndices?.get(item)

    private fun buildIndices(snapshot: List<ItemType>): Map<ItemType, Int> {
        val indices = HashMap<ItemType, Int>(snapshot.size)
        snapshot.forEachIndexed { index, item ->
            // match indexOf by retaining the first equal item
            // if a standalone resolver receives duplicates.
            if (item !in indices) indices[item] = index
        }
        return indices
    }
}

enum class AppearanceIntention {
    Entrance,
    Movement,
    Removal
}

fun <ItemType> StackCreationContext<ItemType>.indexOf(
    item: ItemType
): Int = indexLookup
    ?.indexOf(item)
    ?: stackSnapshot.indexOf(item)

fun <ItemType> StackCreationContext<ItemType>.previousIndexOf(item: ItemType): Int? {
    val prev = previousSnapshot ?: return null
    val lookup = indexLookup
    if (lookup != null) return lookup.previousIndexOf(item)

    val previousIndex = prev.indexOf(item)
    return previousIndex.takeIf { it >= 0 }
}
