package com.nxoim.caif.swipeable

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs

fun Modifier.swipeable(
    detectionConstraint: SwipeConstraint,
    confirmationConstraint: SwipeConstraint = detectionConstraint,
    onStart: GestureScope.(direction: SwipeDirection) -> Unit,
    onProgress: GestureScope.(
        delta: Offset,
        uptimeMillis: Long,
        activationDirection: SwipeDirection
    ) -> Unit,
    onConfirm: GestureScope.(
        velocity: Velocity,
        direction: SwipeDirection
    ) -> Unit,
    onCancel: GestureScope.(velocity: Velocity) -> Unit,
    thresholds: SwipeThresholds = SwipeThresholds.Companion.Default,
    willProcess: (PointerType) -> Boolean = defaultWillProcess,
    interactionSource: MutableInteractionSource? = null,
    key: Any = Unit
): Modifier = pointerInput(
    key,
    thresholds,
    detectionConstraint,
    confirmationConstraint,
    onStart,
    onProgress,
    onConfirm,
    onCancel,
    interactionSource,
    willProcess
) {
    val velocityTracker = VelocityTracker()
    var totalSwipeDeltaPx = Offset.Zero
    var startedSwipingInDirection: SwipeDirection? = null
    var dragInteraction: DragInteraction.Start? = null

    fun resetSwipeStates() {
        totalSwipeDeltaPx = Offset.Zero
        startedSwipingInDirection = null
        velocityTracker.resetTracking()
        dragInteraction = null
    }

    with(GestureScope(this)) {
        detectDragGestures(
            constraint = detectionConstraint,
            isEnabled = willProcess,
            offAxisCancellationDistancePx = thresholds.offAxisCancellation.toPx(),
            touchSlop = thresholds.activation?.toPx() ?: viewConfiguration.touchSlop,
            onDragStart = { down, activationChange, direction ->
                resetSwipeStates()
                startedSwipingInDirection = direction
                velocityTracker.addPointerInputChange(down)

                interactionSource?.let {
                    val interaction = DragInteraction.Start()
                    dragInteraction = interaction
                    it.tryEmit(interaction)
                }

                onStart(direction)
                onProgress(Offset.Zero, activationChange.uptimeMillis, direction)
            },
            onDrag = { pointerChange, dragDelta ->
                totalSwipeDeltaPx += dragDelta
                velocityTracker.addPointerInputChange(pointerChange)
                onProgress(
                    dragDelta,
                    pointerChange.uptimeMillis,
                    startedSwipingInDirection!!
                )
            },
            onDragCancel = {
                startedSwipingInDirection?.let {
                    dragInteraction?.let {
                        interactionSource?.tryEmit(DragInteraction.Cancel(it))
                    }
                    dragInteraction = null

                    onCancel(velocityTracker.calculateVelocity())
                }
            },
            onDragEnd = {
                startedSwipingInDirection?.let {
                    handleDragEnd(
                        totalSwipeDeltaPx = totalSwipeDeltaPx,
                        velocityTracker = velocityTracker,
                        constraint = confirmationConstraint,
                        confirmationVelocityPxPerSecond = velocityThresholdPxPerSecond(
                            thresholds.confirmationVelocity.toPx()
                        ),
                        confirmationMinDistancePx = thresholds.confirmationMinDistance.toPx(),
                        onConfirm = { velocity, direction ->
                            dragInteraction?.let {
                                interactionSource?.tryEmit(DragInteraction.Stop(it))
                            }
                            dragInteraction = null

                            onConfirm(velocity, direction)
                        },
                        onCancel = {
                            dragInteraction?.let {
                                interactionSource?.tryEmit(DragInteraction.Cancel(it))
                            }
                            dragInteraction = null

                            onCancel(it)
                        }
                    )
                }
            }
        )
    }
}

private inline fun GestureScope.handleDragEnd(
    totalSwipeDeltaPx: Offset,
    velocityTracker: VelocityTracker,
    constraint: SwipeConstraint,
    confirmationVelocityPxPerSecond: Float,
    confirmationMinDistancePx: Float,
    onConfirm: GestureScope.(
        velocity: Velocity,
        direction: SwipeDirection
    ) -> Unit,
    onCancel: GestureScope.(velocity: Velocity) -> Unit
) {
    val velocityPxPerSecond = velocityTracker.calculateVelocity()

    val velocitySatisfiesThreshold =
        abs(velocityPxPerSecond.x) >= confirmationVelocityPxPerSecond ||
                abs(velocityPxPerSecond.y) >= confirmationVelocityPxPerSecond

    val distanceSquared = totalSwipeDeltaPx.x *
            totalSwipeDeltaPx.x +
            totalSwipeDeltaPx.y *
            totalSwipeDeltaPx.y
    val distanceSatisfiesThreshold =
        distanceSquared >= (confirmationMinDistancePx * confirmationMinDistancePx)

    if (velocitySatisfiesThreshold || distanceSatisfiesThreshold) {
        val currentSwipingDirection = if (velocitySatisfiesThreshold) {
            constraint.classify(
                deltaX = velocityPxPerSecond.x,
                deltaY = velocityPxPerSecond.y,
            )
        } else {
            constraint.classify(
                deltaX = totalSwipeDeltaPx.x,
                deltaY = totalSwipeDeltaPx.y
            )
        }

        // not null equals allowed
        if (currentSwipingDirection != null) {
            onConfirm(velocityPxPerSecond, currentSwipingDirection)
        } else {
            onCancel(velocityPxPerSecond)
        }
    } else {
        onCancel(velocityPxPerSecond)
    }
}

private suspend fun PointerInputScope.detectDragGestures(
    constraint: SwipeConstraint,
    offAxisCancellationDistancePx: Float,
    touchSlop: Float,
    onDragStart: (
        down: PointerInputChange,
        activationChange: PointerInputChange,
        direction: SwipeDirection,
    ) -> Unit,
    onDragEnd: (change: PointerInputChange) -> Unit,
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    isEnabled: (PointerType) -> Boolean
) {
    val activationVelocityTracker = VelocityTracker()

    awaitEachGesture {
        // observe down early, but arbitrate ownership from movement in Main
        val initialDown = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        if (!isEnabled(initialDown.type)) return@awaitEachGesture

        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Main,
        )

        activationVelocityTracker.resetTracking()
        activationVelocityTracker.addPointerInputChange(down)

        val activation = awaitSwipeActivationOrCancellation(
            down = down,
            pointerId = down.id,
            constraint = constraint,
            offAxisCancellationDistancePx = offAxisCancellationDistancePx,
            touchSlop = touchSlop,
            velocityTracker = activationVelocityTracker,
        )

        if (activation != null) {
            onDragStart(down, activation.change, activation.direction)
            onDrag(activation.change, activation.postSlopOffset)

            val result = drag(
                pointerId = activation.change.id,
                onDrag = { change ->
                    onDrag(change, change.positionChange())
                    change.consume()
                },
                motionConsumed = { it.isConsumed }
            )

            if (result == null) onDragCancel() else onDragEnd(result)
        }
    }
}

private data class SwipeActivation(
    val change: PointerInputChange,
    val postSlopOffset: Offset,
    val direction: SwipeDirection,
)

private data class GesturePickup(
    val pointerId: PointerId,
    val initialPositionChange: Offset,
    val change: PointerInputChange,
)

private suspend fun AwaitPointerEventScope.awaitSwipeActivationOrCancellation(
    down: PointerInputChange,
    pointerId: PointerId,
    constraint: SwipeConstraint,
    offAxisCancellationDistancePx: Float,
    touchSlop: Float = viewConfiguration.touchSlop,
    velocityTracker: VelocityTracker,
): SwipeActivation? {
    var pointer = pointerId
    var totalPositionChange = Offset.Zero
    var startTime = down.uptimeMillis

    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val changeForPointer = event.changes.firstOrNull { it.id == pointer }
        val change = changeForPointer
            ?: (event.changes.fastFirstOrNull { it.pressed }?.also { pointer = it.id } ?: return null)

        if (change.isConsumed) {
            val pickup = awaitGesturePickup(down, pointer) ?: return null
            pointer = pickup.pointerId
            totalPositionChange = pickup.initialPositionChange
            startTime = pickup.change.uptimeMillis
            velocityTracker.resetTracking()
            velocityTracker.addPointerInputChange(pickup.change)
            continue
        }

        if (change.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed } ?: return null
            pointer = otherDown.id
            continue
        }

        if (!change.pressed) return null

        velocityTracker.addPointerInputChange(change)

        val positionChange = change.positionChangeIgnoreConsumed()
        totalPositionChange += positionChange

        val distance = totalPositionChange.getDistance()
        val velocity = velocityTracker.calculateVelocity()
        val timeDelta = change.uptimeMillis - startTime
        val directionFromPosition = constraint.classify(totalPositionChange)
        val directionFromVelocity = if (timeDelta > 0) {
            constraint.classify(velocity)
        } else {
            null
        }
        val direction = directionFromPosition ?: directionFromVelocity

        if (distance > 0f && distance >= touchSlop && direction != null) {
            val postSlopOffset = calculatePostSlopOffset(
                totalPositionChange = totalPositionChange,
                touchSlop = touchSlop,
            )
            change.consume()
            return SwipeActivation(change, postSlopOffset, direction)
        }

        if (
            abs(totalPositionChange.x) >= offAxisCancellationDistancePx ||
            abs(totalPositionChange.y) >= offAxisCancellationDistancePx
        ) {
            if (directionFromPosition == null && directionFromVelocity == null) {
                return null
            }
        }

        awaitPointerEvent(PointerEventPass.Final)
        if (change.isConsumed) {
            val pickup = awaitGesturePickup(down, pointer) ?: return null
            pointer = pickup.pointerId
            totalPositionChange = pickup.initialPositionChange
            startTime = pickup.change.uptimeMillis
            velocityTracker.resetTracking()
            velocityTracker.addPointerInputChange(pickup.change)
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitGesturePickup(
    down: PointerInputChange,
    pointerId: PointerId,
): GesturePickup? {
    var pointer = pointerId

    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Final)
        val pressedChanges = event.changes.filter { it.pressed }
        if (pressedChanges.isEmpty()) return null

        if (event.changes.all { !it.isConsumed }) {
            val change = pressedChanges.firstOrNull { it.id == pointer } ?: pressedChanges.first()
            pointer = change.id
            return GesturePickup(
                pointerId = pointer,
                initialPositionChange = change.position - down.position,
                change = change,
            )
        }
    }
}

internal fun calculatePostSlopOffset(
    totalPositionChange: Offset,
    touchSlop: Float,
): Offset {
    val distance = totalPositionChange.getDistance()
    if (distance == 0f) return Offset.Zero

    return totalPositionChange -
            totalPositionChange / distance * touchSlop.coerceAtLeast(0f)
}

internal suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
    motionConsumed: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    if (currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) {
            val positionChange = it.positionChangeIgnoreConsumed()
            positionChange != Offset.Zero
        }
            ?: return null

        if (motionConsumed(change)) return null

        if (change.changedToUpIgnoreConsumed()) return change

        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes
            .fastFirstOrNull { it.id == pointer }
            ?: return null

        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}


@PublishedApi
internal val defaultWillProcess = fun(type: PointerType): Boolean = true

internal fun velocityThresholdPxPerSecond(thresholdPxPerMillisecond: Float) =
    thresholdPxPerMillisecond * 1_000f
