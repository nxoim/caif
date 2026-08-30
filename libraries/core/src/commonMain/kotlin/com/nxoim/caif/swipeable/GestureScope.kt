package com.nxoim.caif.swipeable

import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.Density

class GestureScope internal constructor(
    private val scope: PointerInputScope,
) : Density by scope {
    val pointerInputScopeSize get() = scope.size
}