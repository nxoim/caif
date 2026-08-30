package com.nxoim.caif.decompose

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.round
import com.arkivanov.essenty.backhandler.BackEvent
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.animateFloat
import com.nxoim.caif.core.animateInt
import com.nxoim.caif.core.animateOffset
import com.nxoim.caif.core.buildSelectableItemAnimation
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.isTopmost
import com.nxoim.caif.springs.smooth
import com.nxoim.caif.springs.springA
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource


fun updatedCupertinoSlideOffset(
    current: Offset,
    delta: Offset,
): Offset {
    val factor = if (current.x > 0f) 1f else 0.2f
    return Offset(current.x + delta.x * factor, 0f)
}

class CupertinoStackAnimation(
    overlayAlphaPerDepth: Float = CupertinoOverlayAlphaPerDepthDefault,
    slideSpringDuration: Duration = 320.milliseconds,
) : BaseItemAnimation<StackAnimationContext>(), SwipeCapability {

    val slide = animateOffset(
        spec = {
            if (position is StackItemPosition.Removed) {
                smooth(duration = slideSpringDuration * 0.9, visibilityThreshold = Offset.VisibilityThreshold)
            } else {
                springA(duration = slideSpringDuration, visibilityThreshold = Offset.VisibilityThreshold)
            }
        },
        stopOnTargetReached = { position is StackItemPosition.Removed },
        value = {
            when (val currentPosition = position) {
                StackItemPosition.PreEntered,
                StackItemPosition.Removed -> Offset(environment.viewportSize.width, 0f)
                is StackItemPosition.Inside -> Offset(
                    (-environment.viewportSize.width * 0.3f) * currentPosition.index,
                    0f,
                )
            }
        },
    )

    val overlayAlpha = animateFloat(
        spec = { springA(duration = slideSpringDuration) },
        value = { cupertinoOverlayAlpha(position, overlayAlphaPerDepth) },
    )

    override val modifier: Modifier = Modifier
        .drawWithContent {
            drawContent()
            if (overlayAlpha.value > 0f) {
                drawRect(Color.Black.copy(alpha = overlayAlpha.value))
            }
        }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                withMotionFrameOfReferencePlacement {
                    placeable.placeRelative(slide.value.round())
                }
            }
        }

    override fun willBeVisible(context: StackAnimationContext): Boolean =
        context.position.isTopmost

    context(density: Density)
    override fun onSwipeUpdate(delta: Offset) {
        slide.value = updatedCupertinoSlideOffset(
            current = slide.value,
            delta = delta,
        )
    }

    context(density: Density)
    override fun onSwipeEnd(velocity: Velocity) {
        slide.prepareVelocity(Offset(velocity.x, 0f))
    }
}

class MaterialStackAnimation : BaseItemAnimation<StackAnimationContext>(), PredictiveBackCapability {
    private val springDuration = 300.milliseconds
    private val velocityTracker = VelocityTracker()
    private var trackingStart: TimeMark? = null
    private var previousTouchPosition = Offset.Zero

    val animatedHorizontalBias = animateFloat(
        spec = { smooth(duration = springDuration, visibilityThreshold = 1f / environment.viewportSize.width) },
        value = { 0f },
    )

    val animatedVerticalDelta = animateFloat(
        spec = { smooth(duration = springDuration, visibilityThreshold = 1f) },
        value = { 0f },
    )

    val animatedScaleFraction = animateFloat(
        spec = { smooth(duration = springDuration, visibilityThreshold = 1f / environment.viewportSize.width) },
        value = { if (position is StackItemPosition.Removed) PredictiveBackScaleAtFullProgress else 1f },
    )

    val animatedAlpha = animateFloat(
        spec = { smooth(duration = springDuration) },
        stopOnTargetReached = { position is StackItemPosition.Removed },
        value = { if (position is StackItemPosition.Removed) 0f else 1f },
    )

    val animatedOverlayAlpha = animateFloat(
        spec = { smooth(duration = springDuration) },
        value = { 0f },
    )

    private val topMostMarker = animateInt(
        spec = { smooth(duration = springDuration) },
        value = { if (position.isTopmost) 1 else 0 },
    )

    val isTopmost get() = topMostMarker.value == 1
    private var swipeEdge = BackEvent.SwipeEdge.UNKNOWN
    private var currentHorizontalBias = 0f
    private var currentUnderlyingHorizontalBias = 0f
    private var currentVerticalOffset = 0f

    override val modifier: Modifier = Modifier
        .drawWithContent {
            drawContent()
            if (animatedOverlayAlpha.value > 0f) {
                drawRect(Color.Black.copy(alpha = animatedOverlayAlpha.value.coerceIn(0f, 1f)))
            }
        }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                withMotionFrameOfReferencePlacement {
                    placeable.placeWithLayer(
                        x = (animatedHorizontalBias.value * placeable.width).roundToInt(),
                        y = animatedVerticalDelta.value.roundToInt(),
                    ) {
                        scaleX = animatedScaleFraction.value
                        scaleY = animatedScaleFraction.value
                        alpha = animatedAlpha.value.coerceIn(0f, 1f)
                    }
                }
            }
        }

    override fun willBeVisible(context: StackAnimationContext): Boolean =
        context.position.isTopmost

    override fun onStart(
        progress: Float,
        touchPosition: Offset,
        swipeEdge: BackEvent.SwipeEdge
    ) {
        velocityTracker.resetTracking()
        trackingStart = TimeSource.Monotonic.markNow()
        previousTouchPosition = touchPosition
        this.swipeEdge = swipeEdge
        currentHorizontalBias = predictiveBackHorizontalBias(progress, swipeEdge)
        currentUnderlyingHorizontalBias = predictiveBackUnderlyingHorizontalBias()
        currentVerticalOffset = 0f

        animatedHorizontalBias.value = if (isTopmost) currentHorizontalBias else currentUnderlyingHorizontalBias
        animatedScaleFraction.value = if (isTopmost) predictiveBackScale(progress) else predictiveBackUnderlyingScale(progress)
        animatedVerticalDelta.value = currentVerticalOffset
        animatedOverlayAlpha.value = if (isTopmost) 0f else predictiveBackUnderlyingOverlayAlpha(progress)
        record(progress, touchPosition.y)
    }

    override fun onProgress(
        progress: Float,
        touchPosition: Offset,
        swipeEdge: BackEvent.SwipeEdge
    ) {
        val delta = touchPosition - previousTouchPosition
        previousTouchPosition = touchPosition
        this.swipeEdge = swipeEdge
        currentHorizontalBias = predictiveBackHorizontalBias(progress, swipeEdge)
        currentUnderlyingHorizontalBias = predictiveBackUnderlyingHorizontalBias()
        currentVerticalOffset += delta.y / PredictiveBackVerticalDamping

        animatedVerticalDelta.value = currentVerticalOffset
        if (isTopmost) {
            animatedHorizontalBias.value = currentHorizontalBias
            animatedScaleFraction.value = predictiveBackScale(progress)
            animatedOverlayAlpha.value = 0f
        } else {
            animatedHorizontalBias.value = currentUnderlyingHorizontalBias
            animatedScaleFraction.value = predictiveBackUnderlyingScale(progress)
            animatedOverlayAlpha.value = predictiveBackUnderlyingOverlayAlpha(progress)
        }
        record(progress, touchPosition.y)
    }

    override fun onEnd() {
        val velocity = velocityTracker.calculateVelocity()
        if (isTopmost) {
            val direction = predictiveBackHorizontalBiasDirection(swipeEdge)
            val horizontalVelocity = velocity.x * direction
            animatedHorizontalBias.prepareVelocity(horizontalVelocity)
        }
        animatedVerticalDelta.prepareVelocity(velocity.y / PredictiveBackVerticalDamping)
    }

    private fun record(progress: Float, touchY: Float) {
        val timestamp = trackingStart?.elapsedNow()?.inWholeMilliseconds ?: return
        velocityTracker.addPosition(timestamp, Offset(progress, touchY))
    }
}

fun cupertinoStackAnimation(
    overlayAlphaPerDepth: Float = CupertinoOverlayAlphaPerDepthDefault,
    slideSpringDuration: Duration = 320.milliseconds,
): ItemAnimation<StackAnimationContext> = CupertinoStackAnimation(
    overlayAlphaPerDepth = overlayAlphaPerDepth,
    slideSpringDuration = slideSpringDuration,
)

fun materialStackAnimation(): ItemAnimation<StackAnimationContext> =
    MaterialStackAnimation()

fun adaptiveStackAnimation(
    overlayAlphaPerDepth: Float = CupertinoOverlayAlphaPerDepthDefault,
    slideSpringDuration: Duration = 320.milliseconds,
): ItemAnimation<StackAnimationContext> = selectableStackAnimation(
    swipe = {
        CupertinoStackAnimation(
            overlayAlphaPerDepth = overlayAlphaPerDepth,
            slideSpringDuration = slideSpringDuration,
        )
    },
    predictiveBack = {
        MaterialStackAnimation()
    },
)

fun selectableStackAnimation(
    swipe: () -> ItemAnimation<StackAnimationContext> = { CupertinoStackAnimation() },
    predictiveBack: () -> ItemAnimation<StackAnimationContext> = { MaterialStackAnimation() },
): ItemAnimation<StackAnimationContext> = buildSelectableItemAnimation {
    val swipeSelector = selectOnCapability<SwipeCapability> {
        swipe()
    }
    selectOnCapability<PredictiveBackCapability> {
        predictiveBack()
    }
    defaultSelector(swipeSelector)
}

fun adaptiveOverlayAlpha(
    position: StackItemPosition,
    overlayAlphaPerDepth: Float = AdaptiveOverlayAlphaPerDepthDefault,
): Float = when (position) {
    StackItemPosition.PreEntered,
    StackItemPosition.Removed -> 0f

    is StackItemPosition.Inside ->
        (position.index * overlayAlphaPerDepth).coerceAtMost(1f)
}

fun cupertinoOverlayAlpha(
    position: StackItemPosition,
    overlayAlphaPerDepth: Float = AdaptiveOverlayAlphaPerDepthDefault,
): Float = adaptiveOverlayAlpha(position, overlayAlphaPerDepth)

private fun predictiveBackScale(progress: Float): Float =
    1f - (1f - PredictiveBackScaleAtFullProgress) * progress.coerceIn(0f, 1f)

private fun predictiveBackUnderlyingScale(progress: Float): Float =
    1f - (1f - PredictiveBackUnderlyingScaleAtFullProgress) * progress.coerceIn(0f, 1f)

private fun predictiveBackUnderlyingOverlayAlpha(progress: Float): Float =
    (1f - progress.coerceIn(0f, 1f)) * PredictiveBackUnderlyingInitialOverlayAlpha

private fun predictiveBackHorizontalBias(
    progress: Float,
    swipeEdge: BackEvent.SwipeEdge,
): Float = predictiveBackHorizontalBiasDirection(swipeEdge) *
        PredictiveBackHorizontalOffsetAtFullProgress * progress.coerceIn(0f, 1f)

private fun predictiveBackHorizontalBiasDirection(
    swipeEdge: BackEvent.SwipeEdge,
): Float = when (swipeEdge) {
    BackEvent.SwipeEdge.LEFT -> 1f
    BackEvent.SwipeEdge.RIGHT -> -1f
    BackEvent.SwipeEdge.UNKNOWN -> 0f
}

private fun predictiveBackUnderlyingHorizontalBias(): Float =
    -PredictiveBackUnderlyingHorizontalOffset

private const val AdaptiveOverlayAlphaPerDepthDefault = 0.3f
private const val CupertinoOverlayAlphaPerDepthDefault = 0.3f
private const val PredictiveBackScaleAtFullProgress = 0.85f
private const val PredictiveBackUnderlyingScaleAtFullProgress = 0.90f
private const val PredictiveBackHorizontalOffsetAtFullProgress = 0.15f
private const val PredictiveBackUnderlyingHorizontalOffset = 0.20f
private const val PredictiveBackUnderlyingInitialOverlayAlpha = 0.25f
private const val PredictiveBackVerticalDamping = 10f
