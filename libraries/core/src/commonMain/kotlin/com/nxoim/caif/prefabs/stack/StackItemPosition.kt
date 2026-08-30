package com.nxoim.caif.prefabs.stack

sealed interface StackItemPosition {
    data object PreEntered : StackItemPosition
    data object Removed : StackItemPosition
    data class Inside(val index: Int, val previousIndex: Int?) : StackItemPosition
}

val StackItemPosition.isTopmost get() = this is StackItemPosition.Inside && this.index == 0