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

@Immutable
data class StackCreationContext<ItemType>(
    val stackSnapshot: List<ItemType>,
    val previousSnapshot: List<ItemType>? = null,
    val intention: AppearanceIntention,
)

enum class AppearanceIntention {
    Entrance,
    Movement,
    Removal
}

fun <ItemType> StackCreationContext<ItemType>.indexOf(
    item: ItemType
): Int = stackSnapshot.indexOf(item)

fun <ItemType> StackCreationContext<ItemType>.previousIndexOf(item: ItemType): Int? {
    val prev = previousSnapshot ?: return null
    val previousIndex = prev.indexOf(item)
    return previousIndex.takeIf { it >= 0 }
}
