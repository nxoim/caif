package com.nxoim.caif.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler

@Composable
fun StackBackHandler(
    backHandler: BackHandler,
    canPop: Boolean,
    predictiveBackDispatcher: PredictiveBackCapabilityDispatcher,
    onPop: () -> Unit,
) {
    val currentCanPop = rememberUpdatedState(canPop)
    val currentOnPop = rememberUpdatedState(onPop)
    val callback = remember(backHandler, predictiveBackDispatcher) {
        FlowStackBackCallback(
            canPop = { currentCanPop.value },
            predictiveBackDispatcher = predictiveBackDispatcher,
            onPop = { currentOnPop.value() },
        )
    }

    SideEffect {
        callback.isEnabled = canPop
    }

    DisposableEffect(backHandler, callback) {
        backHandler.register(callback)
        onDispose { backHandler.unregister(callback) }
    }
}

private class FlowStackBackCallback(
    private val canPop: () -> Boolean,
    private val predictiveBackDispatcher: PredictiveBackCapabilityDispatcher,
    private val onPop: () -> Unit,
) : BackCallback(isEnabled = false) {
    private var isPredictiveBackActive = false

    override fun onBackStarted(backEvent: BackEvent) {
        if (!canPop()) return

        if (isPredictiveBackActive) {
            onBackCancelled()
        }

        predictiveBackDispatcher.onStart(
            progress = backEvent.progress,
            touchPosition = Offset(backEvent.touchX, backEvent.touchY),
            swipeEdge = backEvent.swipeEdge,
        )
        isPredictiveBackActive = true
    }

    override fun onBackProgressed(backEvent: BackEvent) {
        if (!isPredictiveBackActive) return

        predictiveBackDispatcher.onProgress(
            progress = backEvent.progress,
            touchPosition = Offset(backEvent.touchX, backEvent.touchY),
            swipeEdge = backEvent.swipeEdge,
        )
    }

    override fun onBackCancelled() {
        if (!isPredictiveBackActive) return

        predictiveBackDispatcher.onEnd()
        isPredictiveBackActive = false
    }

    override fun onBack() {
        val wasPredictiveBackActive = isPredictiveBackActive
        isPredictiveBackActive = false

        if (canPop()) {
            onPop()
        }
        if (wasPredictiveBackActive) {
            predictiveBackDispatcher.onEnd()
        }
    }
}
