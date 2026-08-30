package com.nxoim.caif.decompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import com.nxoim.caif.prefabs.stack.BaseCapabilityDispatcher
import com.nxoim.caif.prefabs.stack.StackCycleController

interface SwipeCapability {
    context(density: Density)
    fun onSwipeUpdate(delta: Offset)

    context(density: Density)
    fun onSwipeEnd(velocity: Velocity)
}

class SwipeCapabilityDispatcher(
    cycleController: StackCycleController
) : BaseCapabilityDispatcher<SwipeCapability>(
    SwipeCapability::class,
    cycleController
) {
    fun onStart() = startCycle()

    context(density: Density)
    fun onUpdate(delta: Offset) = forEachAffectedItemsCapability { onSwipeUpdate(delta) }

    context(density: Density)
    fun onEnd(velocity: Velocity) {
        forEachAffectedItemsCapability { onSwipeEnd(velocity) }
        progressCycle()
    }
}
