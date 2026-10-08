package com.nxoim.sample.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.core.animateFloat
import com.nxoim.caif.core.animateOffset
import com.nxoim.caif.decompose.StackAnimationContext
import com.nxoim.caif.decompose.SwipeCapability
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.isTopmost
import com.nxoim.caif.springs.smooth
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class ExpansionSwipeStackAnimation(
    private val maximumDistance: Dp = 800.dp,
    private val scaleReduction: Float = ExpandedSwipeScaleReductionDefault,
    private val minimumScale: Float = ExpandedSwipeMinimumScaleDefault,
    layoutExpansion: Float = ExpandedSwipeLayoutExpansionDefault,
    springDuration: Duration = 300.milliseconds,
) : BaseItemAnimation<StackAnimationContext>(), SwipeCapability {
    private val StackAnimationContext.scaleOnePixel get() = 1f / maxOf(environment.viewportSize.width, environment.viewportSize.height)

    private val offset = animateOffset(
        spec = { smooth(duration = springDuration, visibilityThreshold = Offset.Zero) },
        value = { Offset.Zero },
    )

    private val scale = animateFloat(
        spec = { smooth(duration = springDuration, visibilityThreshold = scaleOnePixel) },
        value = { if (position is StackItemPosition.Removed) 0.6f else 1f },
    )

    private val animatedAlpha = animateFloat(
        spec = { smooth(duration = springDuration) },
        stopOnTargetReached = { position is StackItemPosition.Removed },
        value = { if (position is StackItemPosition.Removed) 0f else 1f },
    )

    private val animatedLayoutExpansion = animateFloat(
        spec = { smooth(duration = springDuration, visibilityThreshold = scaleOnePixel) },
        value = {
            if ((position as? StackItemPosition.Inside)?.index == 1)
                layoutExpansion
            else
                0f
        },
    )

    override val modifier = Modifier
        .layout { measurable, constraints ->
            val expansionFactor = 1f + animatedLayoutExpansion.value

            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = (constraints.minWidth * expansionFactor).roundToInt(),
                    maxWidth = if (constraints.hasBoundedWidth)
                        (constraints.maxWidth * expansionFactor).roundToInt() else constraints.maxWidth,
                    minHeight = (constraints.minHeight * expansionFactor).roundToInt(),
                    maxHeight = if (constraints.hasBoundedHeight)
                        (constraints.maxHeight * expansionFactor).roundToInt() else constraints.maxHeight,
                )
            )
            val width = constraints.constrainWidth((placeable.width / expansionFactor).roundToInt())
            val height =
                constraints.constrainHeight((placeable.height / expansionFactor).roundToInt())

            layout(width, height) {
                withMotionFrameOfReferencePlacement {
                    val swipeOffset = offset.value.round()

                    placeable.placeRelativeWithLayer(
                        x = swipeOffset.x + (width - placeable.width) / 2,
                        y = swipeOffset.y + (height - placeable.height) / 2,
                    ) {
                        scaleX = scale.value / expansionFactor
                        scaleY = scale.value / expansionFactor
                        alpha = animatedAlpha.value.coerceIn(0f, 1f)
                    }
                }
            }
        }

    override fun StackAnimationContext.isVisibleWhen(): Boolean = position.isTopmost


    context(density: Density)
    override fun onSwipeUpdate(delta: Offset) {
        if (currentContext?.position?.isTopmost == true) {
            offset.value += delta
            val maxPx = with(density) { maximumDistance.toPx() }
            scale.value = (1f - swipeDistanceFraction(offset.value, maxPx) * scaleReduction)
                .coerceAtLeast(minimumScale)
        }
    }

    context(density: Density)
    override fun onSwipeEnd(velocity: Velocity) {
        if (currentContext?.position?.isTopmost == true) {
            offset.prepareVelocity(Offset(velocity.x, velocity.y))
        }
    }
}

private fun swipeDistanceFraction(offset: Offset, maximumDistance: Float): Float {
    if (maximumDistance <= 0f) return 0f
    return sqrt(offset.x * offset.x + offset.y * offset.y) / maximumDistance
}

private const val ExpandedSwipeScaleReductionDefault = 0.35f
private const val ExpandedSwipeMinimumScaleDefault = 0.85f
private const val ExpandedSwipeLayoutExpansionDefault = 0.1f
