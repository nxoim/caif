package com.nxoim.caif.prefabs.stack

import androidx.compose.runtime.Immutable

@Immutable
interface ContextResolver<ItemType, Key : Any, Context, CreationContext> {
    fun keyFor(itemType: ItemType): Key

    fun buildContexts(
        stack: List<ItemType>,
        previousStack: List<ItemType>?,
        treatNewEnteringAsPreparing: Boolean,
        recalculateEnteringToMoving: Boolean,
        previousContexts: Map<Key, Context>? = null
    ): Map<Key, Pair<Context, CreationContext>>
}