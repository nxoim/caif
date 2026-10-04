package com.nxoim.caif.prefabs.stack

/**
 *  Identifies which end of a logical stack snapshot contains
 *  its topmost item.
 */
enum class StackOrder {
    TopmostFirst,
    TopmostLast;

    fun <Item> topmostItem(stack: List<Item>): Item? = when (this) {
        TopmostFirst -> stack.firstOrNull()
        TopmostLast -> stack.lastOrNull()
    }

    internal fun depthFromTop(index: Int, lastIndex: Int): Int = when (this) {
        TopmostFirst -> index
        TopmostLast -> lastIndex - index
    }
}
