package com.nxoim.sample.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.animateFloat
import com.nxoim.caif.core.animateOffset
import com.nxoim.caif.decompose.MaterialStackAnimation
import com.nxoim.caif.decompose.StackAnimationContext
import com.nxoim.caif.decompose.SwipeCapability
import com.nxoim.caif.decompose.selectableStackAnimation
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.isTopmost
import com.nxoim.caif.springs.smooth
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ExpansionSwipeStackAnimation(
    val maximumDistance: Dp = 800.dp,
    val scaleReduction: Float = ExpandedSwipeScaleReductionDefault,
    val minimumScale: Float = ExpandedSwipeMinimumScaleDefault,
    springDuration: Duration = 300.milliseconds,
) : BaseItemAnimation<StackAnimationContext>(), SwipeCapability {
    val offset = animateOffset(
        spec = { smooth(duration = springDuration) },
        value = { Offset.Zero },
    )

    val scale = animateFloat(
        spec = { smooth(duration = springDuration) },
        value = { if (position is StackItemPosition.Removed) 0.6f else 1f },
    )

    val animatedAlpha = animateFloat(
        spec = { smooth(duration = springDuration) },
        stopOnTargetReached = { position is StackItemPosition.Removed },
        value = { if (position is StackItemPosition.Removed) 0f else 1f },
    )

    override val modifier = Modifier
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)

            layout(placeable.width, placeable.height) {
                withMotionFrameOfReferencePlacement {
                    placeable.placeRelativeWithLayer(offset.value.round()) {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = animatedAlpha.value.coerceIn(0f, 1f)
                    }
                }
            }
        }

    override fun willBeVisible(context: StackAnimationContext): Boolean =
        context.position.isTopmost

    context(density: Density)
    override fun onSwipeUpdate(delta: Offset) {
        offset.value += delta
        val maxPx = with(density) { maximumDistance.toPx() }
        scale.value = (1f - swipeDistanceFraction(offset.value, maxPx) * scaleReduction)
            .coerceAtLeast(minimumScale)
    }

    context(density: Density)
    override fun onSwipeEnd(velocity: Velocity) {
        offset.prepareVelocity(Offset(velocity.x, velocity.y))
    }
}

fun expansionSwipeStackAnimation(
    maximumDistance: Dp = 800.dp,
    scaleReduction: Float = ExpandedSwipeScaleReductionDefault,
    minimumScale: Float = ExpandedSwipeMinimumScaleDefault,
    springDuration: Duration = 300.milliseconds,
): ItemAnimation<StackAnimationContext> = selectableStackAnimation(
    swipe = {
        ExpansionSwipeStackAnimation(
            maximumDistance = maximumDistance,
            scaleReduction = scaleReduction,
            minimumScale = minimumScale,
            springDuration = springDuration,
        )
    },
    predictiveBack = {
        MaterialStackAnimation()
    },
)

private const val ExpandedSwipeScaleReductionDefault = 0.35f
private const val ExpandedSwipeMinimumScaleDefault = 0.85f

private fun swipeDistanceFraction(offset: Offset, maximumDistance: Float): Float {
    if (maximumDistance <= 0f) return 0f
    return sqrt(offset.x * offset.x + offset.y * offset.y) / maximumDistance
}
