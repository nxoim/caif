package com.nxoim.caif.decompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.essenty.backhandler.BackEvent
import com.nxoim.caif.springs.smooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun Modifier.decomposeKeyBackHandler(
    canPop: Boolean,
    onPop: () -> Unit,
    predictiveBackDispatcher: PredictiveBackCapabilityDispatcher,
    animationSpec: AnimationSpec<Float> = smooth(),
    initialVelocity: Float = 20f,
    scope: CoroutineScope = rememberCoroutineScope(),
): Modifier {
    val currentCanPop = rememberUpdatedState(canPop)
    val currentOnPop = rememberUpdatedState(onPop)
    val isHandling = remember { mutableStateOf(false) }
    val isKeyPressed = remember { mutableStateOf(false) }
    val animationJob = remember { mutableStateOf<Job?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(canPop) {
        if (canPop) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isHandling.value) {
                isHandling.value = false
                isKeyPressed.value = false
                animationJob.value?.cancel()
                animationJob.value = null
                predictiveBackDispatcher.onEnd()
            }
        }
    }

    return this
        .focusRequester(focusRequester)
        .focusable()
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    runCatching { focusRequester.requestFocus() }
                },
            )
        }
        .onKeyEvent { keyEvent ->
            when (keyEvent.type) {
                KeyEventType.KeyDown -> {
                    if (keyEvent.key == Key.Escape) {
                        if (!isKeyPressed.value && currentCanPop.value && !isHandling.value) {
                            isKeyPressed.value = true
                            isHandling.value = true
                            animationJob.value = scope.launch {
                                try {
                                    predictiveBackDispatcher.onStart(
                                        progress = 0f,
                                        touchPosition = Offset.Zero,
                                        swipeEdge = BackEvent.SwipeEdge.LEFT,
                                    )

                                    val animatable = Animatable(0f)
                                    animatable.animateTo(
                                        targetValue = 1f,
                                        initialVelocity = initialVelocity,
                                        animationSpec = animationSpec,
                                    ) {
                                        predictiveBackDispatcher.onProgress(
                                            progress = value,
                                            touchPosition = Offset(0f, 0f),
                                            swipeEdge = BackEvent.SwipeEdge.LEFT,
                                        )
                                    }
                                } finally {
                                    // If animation completed while key is still held, maintain preview
                                }
                            }
                            true
                        } else if (isKeyPressed.value) {
                            // Key is being held down; consume OS key-repeat events without re-triggering
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                KeyEventType.KeyUp -> {
                    if (keyEvent.key == Key.Escape) {
                        isKeyPressed.value = false
                        if (isHandling.value) {
                            isHandling.value = false
                            animationJob.value?.cancel()
                            animationJob.value = null
                            currentOnPop.value()
                            predictiveBackDispatcher.onEnd()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
}
