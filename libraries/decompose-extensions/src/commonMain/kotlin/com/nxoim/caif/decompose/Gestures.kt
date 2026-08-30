package com.nxoim.caif.decompose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import com.nxoim.caif.swipeable.SwipeDirection

interface BackGestureParticipant {
    fun canHandle(): Boolean
    fun onStart()

    context(density: Density)
    fun onProgress(delta: Offset, uptimeMillis: Long, activationDirection: SwipeDirection)

    context(density: Density)
    fun onConfirm(velocity: Velocity)

    context(density: Density)
    fun onCancel(velocity: Velocity)
}

val LocalParentBackGesture = staticCompositionLocalOf<BackGestureParticipant?> { null }

class StackGestureParticipant(
    private val canPop: () -> Boolean,
    private val parent: BackGestureParticipant?,
    private val startLocal: () -> Unit,
    private val updateLocal: Density.(Offset, Long, SwipeDirection) -> Unit,
    private val confirmLocal: Density.(Velocity) -> Unit,
    private val cancelLocal: Density.(Velocity) -> Unit,
) : BackGestureParticipant {
    private var owner: BackGestureParticipant? = null

    override fun canHandle(): Boolean = canPop() || parent?.canHandle() == true

    override fun onStart() {
        owner = if (canPop()) {
            startLocal()
            this
        } else {
            parent?.also { it.onStart() }
        }
    }

    context(density: Density)
    override fun onProgress(
        delta: Offset,
        uptimeMillis: Long,
        activationDirection: SwipeDirection,
    ) {
        when (val activeOwner = owner) {
            this -> density.updateLocal(delta, uptimeMillis, activationDirection)
            null -> Unit
            else -> activeOwner.onProgress(delta, uptimeMillis, activationDirection)
        }
    }

    context(density: Density)
    override fun onConfirm(velocity: Velocity) {
        when (val activeOwner = owner) {
            this -> density.confirmLocal(velocity)
            null -> Unit
            else -> activeOwner.onConfirm(velocity)
        }
        owner = null
    }

    context(density: Density)
    override fun onCancel(velocity: Velocity) {
        when (val activeOwner = owner) {
            this -> density.cancelLocal(velocity)
            null -> Unit
            else -> activeOwner.onCancel(velocity)
        }
        owner = null
    }
}
