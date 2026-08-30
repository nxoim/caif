package com.nxoim.sample.ui.tasks.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nxoim.sample.ui.theme.fastSmoothSpring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TaskSwipeActions(
    state: TaskSwipeActionsState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .onSizeChanged { state.updateContainerWidth(it.width) }
            .actionAnimation(state),
    ) {
        if (!state.isIdle) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onSizeChanged { state.updateActionGroupWidth(it.width) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArchiveAction(
                    onClick = {
                        state.dismiss(TaskSwipeAction.Archive)
                    },
                )
                DeleteAction(
                    onClick = {
                        state.dismiss(TaskSwipeAction.Delete)
                    },
                )
            }
        }
        Box(Modifier.motionFrameOffset(state)) {
            content()
        }
    }
}

private fun Modifier.motionFrameOffset(
    state: TaskSwipeActionsState
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        withMotionFrameOfReferencePlacement {
            placeable.place(state.foregroundOffsetX.roundToInt(), 0)
        }
    }
}

private fun Modifier.actionAnimation(state: TaskSwipeActionsState): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            placeable.width,
            (placeable.height * (1f - state.dismissalProgress)).roundToInt()
        ) {
            withMotionFrameOfReferencePlacement {
                placeable.placeRelativeWithLayer(0, 0) {
                    alpha = 1f - state.dismissalProgress
                }
            }
        }
    }

@Composable
private fun ArchiveAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(modifier = modifier, onClick = onClick) {
        Icon(Icons.Default.Archive, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Archive")
    }
}

@Composable
private fun DeleteAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Icon(Icons.Default.Delete, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Delete")
    }
}


object TaskSwipeActionsDefaults {
    fun settleMotionSpec(): AnimationSpec<Float> = fastSmoothSpring()
}

@Stable
class TaskSwipeActionsState internal constructor(
    private val coroutineScope: CoroutineScope,
    private val settleMotionSpec: AnimationSpec<Float>,
    layoutDirection: LayoutDirection,
    private val onDismiss: (TaskSwipeAction) -> Boolean,
) {
    private var actionGroupWidthPx by mutableIntStateOf(0)
    private var containerWidthPx by mutableIntStateOf(0)
    private val revealDirection = if (layoutDirection == LayoutDirection.Ltr) -1f else 1f
    private var settleJob: Job? = null
    private var dismissalJob: Job? = null

    var foregroundOffsetX by mutableFloatStateOf(0f)
        private set

    var dismissalProgress by mutableFloatStateOf(0f)
        private set

    val isIdle: Boolean
        get() = foregroundOffsetX == 0f

    internal fun updateActionGroupWidth(width: Int) {
        actionGroupWidthPx = width
    }

    internal fun updateContainerWidth(width: Int) {
        containerWidthPx = width
    }

    fun onSwipeDelta(deltaX: Float) {
        settleJob?.cancel()
        foregroundOffsetX += deltaX
    }

    fun onSwipeEnd(velocity: Velocity) {
        val revealOffset = revealDirection * actionGroupWidthPx
        val target = if (
            actionGroupWidthPx > 0 && foregroundOffsetX * revealDirection >= actionGroupWidthPx / 2f
        ) {
            revealOffset
        } else {
            0f
        }
        settleTo(target, velocity.x)
    }

    fun dismiss(action: TaskSwipeAction) {
        if (dismissalJob?.isActive == true) return

        settleJob?.cancel()
        dismissalJob = coroutineScope.launch {
            coroutineScope {
                launch {
                    animate(
                        initialValue = foregroundOffsetX,
                        targetValue = revealDirection * containerWidthPx,
                        animationSpec = settleMotionSpec,
                    ) { value, _ ->
                        foregroundOffsetX = value
                    }
                }
                launch {
                    animate(
                        initialValue = dismissalProgress,
                        targetValue = 1f,
                        animationSpec = settleMotionSpec,
                    ) { value, _ ->
                        dismissalProgress = value
                    }
                }
            }
            if (!onDismiss(action)) {
                animate(
                    initialValue = dismissalProgress,
                    targetValue = 0f,
                    animationSpec = settleMotionSpec,
                ) { value, _ ->
                    dismissalProgress = value
                }
                settleTo(0f, 0f)
            }
        }
    }

    private fun settleTo(target: Float, initialVelocity: Float) {
        settleJob?.cancel()
        settleJob = coroutineScope.launch {
            animate(
                initialValue = foregroundOffsetX,
                targetValue = target,
                initialVelocity = initialVelocity,
                animationSpec = settleMotionSpec,
            ) { value, _ ->
                foregroundOffsetX = value
            }
        }
    }
}

enum class TaskSwipeAction {
    Archive,
    Delete,
}

@Composable
fun rememberTaskSwipeActionsState(
    onDismiss: (TaskSwipeAction) -> Boolean,
    settleMotionSpec: AnimationSpec<Float> = TaskSwipeActionsDefaults.settleMotionSpec(),
): TaskSwipeActionsState {
    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    return remember(coroutineScope, layoutDirection, settleMotionSpec) {
        TaskSwipeActionsState(
            coroutineScope = coroutineScope,
            settleMotionSpec = settleMotionSpec,
            layoutDirection = layoutDirection,
            onDismiss = { currentOnDismiss.value(it) },
        )
    }
}
