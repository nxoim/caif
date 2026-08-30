package com.nxoim.caif.decompose

import androidx.compose.ui.geometry.Offset
import com.arkivanov.essenty.backhandler.BackEvent
import com.nxoim.caif.prefabs.stack.BaseCapabilityDispatcher
import com.nxoim.caif.prefabs.stack.StackCycleController

interface PredictiveBackCapability {
    fun onStart(progress: Float, touchPosition: Offset, swipeEdge: BackEvent.SwipeEdge)
    fun onProgress(progress: Float, touchPosition: Offset, swipeEdge: BackEvent.SwipeEdge)
    fun onEnd()
}

class PredictiveBackCapabilityDispatcher(
    cycleController: StackCycleController
) : BaseCapabilityDispatcher<PredictiveBackCapability>(
    PredictiveBackCapability::class,
    cycleController
) {
    fun onStart(progress: Float, touchPosition: Offset, swipeEdge: BackEvent.SwipeEdge) {
        startCycle()
        forEachAffectedItemsCapability { onStart(progress, touchPosition, swipeEdge) }
    }

    fun onProgress(progress: Float, touchPosition: Offset, swipeEdge: BackEvent.SwipeEdge) {
        forEachAffectedItemsCapability { onProgress(progress, touchPosition, swipeEdge) }
    }

    fun onEnd() {
        forEachAffectedItemsCapability { onEnd() }
        progressCycle()
    }
}
