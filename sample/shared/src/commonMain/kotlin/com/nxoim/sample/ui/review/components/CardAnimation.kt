package com.nxoim.sample.ui.review.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.EaseInQuint
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEach
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.animateFloat
import com.nxoim.caif.core.animateOffset
import com.nxoim.caif.core.base.AnimatedOffset
import com.nxoim.caif.core.base.AnimatedValue
import com.nxoim.caif.core.base.MutableAnimatedValue
import com.nxoim.caif.core.getAndSelectCapability
import com.nxoim.caif.decompose.SwipeCapability
import com.nxoim.caif.springs.bouncy
import com.nxoim.caif.springs.smooth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class CardStackItemAnimation : StabilizedItemAnimation<CardContext>(), SwipeCapability {
    private var layoutDirection = LayoutDirection.Ltr

    val offset = animateOffset(spec = { smooth() }) {
        layoutDirection = environment.layoutDirection
        when (position) {
            CardContext.Position.PreEntered -> Offset(0f, environment.maxHeight)
            CardContext.Position.Inside -> Offset(0f, this.depth * -100f)
            CardContext.Position.Accepted -> Offset(environment.maxWidth, 0f)
            CardContext.Position.Declined -> Offset(-environment.maxWidth, 0f)
        }
    }

    val scale = animateFloat(spec = { smooth() }) {
        if (position == CardContext.Position.Inside) {
            1f - (depth * 0.05f)
        } else {
            1f
        }
    }

    val animatedAlpha = animateFloat {
        val threshold = 2f
        EaseInQuart.transform(threshold / depth.coerceAtLeast(1))
    }

    val stabilized = offset.stabilize(
        spec = bouncy(),
        targetAnimation = AnimatedOffset(),
        transform = { it * scale.value },
    )


    override val modifier: Modifier = Modifier
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                withMotionFrameOfReferencePlacement {
                    placeable.placeRelative(offset.value.round())
                }
            }
        }
        .drawWithContent {
            // This keeps the tilt in the card's coordinate space, so swipe input still
            // receives the untransformed pointer coordinates.
            val pushGradeX = stabilized.velocity.y * 0.0003f
            val pushGradeY = stabilized.velocity.x * 0.0003f
            val cameraDistance = 16.dp.toPx()

            withTransform(
                transformBlock = {
                    transform(
                        rotate(
                            pushGradeX,
                            -pushGradeY,
                            cameraDistance,
                        ),
                    )
                },
            ) {
                this@drawWithContent.drawContent()
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin(0.5f, 0.5f)
            this.alpha = animatedAlpha.value
        }

    override fun willBeVisible(context: CardContext): Boolean =
        context.position == CardContext.Position.Inside && context.depth < 6

    context(density: Density)
    override fun onSwipeUpdate(delta: Offset) {
        val grade = EaseInQuint.transform(scale.value)
        val logicalDeltaX = if (layoutDirection == LayoutDirection.Ltr) delta.x else -delta.x
        offset.value += Offset(logicalDeltaX, delta.y) * grade
    }

    context(density: Density)
    override fun onSwipeEnd(velocity: Velocity) {
        val grade = EaseInQuint.transform(scale.value)
        val logicalVelocityX = if (layoutDirection == LayoutDirection.Ltr) velocity.x else -velocity.x
        offset.prepareVelocity(Offset(logicalVelocityX, velocity.y) * grade)
    }
}

internal fun cardAnimation(): ItemAnimation<CardContext> = CardStackItemAnimation()

private fun DrawTransform.rotate(
    rotX: Float,
    rotY: Float,
    cameraDistance: Float,
): Matrix {
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    val radX = toRadians(rotX.toDouble()).toFloat()
    val radY = toRadians(rotY.toDouble()).toFloat()

    val sinX = sin(radX)
    val cosX = cos(radX)
    val sinY = sin(radY)
    val cosY = cos(radY)

    val matrixX = floatArrayOf(
        cameraDistance,
        -centerX * sinX,
        centerX * centerY * sinX,
        0f,
        cosX * cameraDistance - centerY * sinX,
        centerY * (cameraDistance * (1 - cosX) + centerY * sinX),
        0f,
        -sinX,
        cameraDistance + centerY * sinX,
    )

    val matrixY = floatArrayOf(
        cosY * cameraDistance - centerX * sinY,
        0f,
        centerX * (cameraDistance * (1 - cosY) + centerX * sinY),
        -centerY * sinY,
        cameraDistance,
        centerY * centerX * sinY,
        -sinY,
        0f,
        cameraDistance + centerX * sinY,
    )

    val result = FloatArray(9)
    for (row in 0..2) {
        for (col in 0..2) {
            result[row * 3 + col] = (0..2).fold(0f) { sum, k ->
                sum + matrixY[row * 3 + k] * matrixX[k * 3 + col]
            }
        }
    }

    return Matrix().apply {
        values[0] = result[0]
        values[4] = result[1]
        values[12] = result[2]
        values[1] = result[3]
        values[5] = result[4]
        values[13] = result[5]
        values[3] = result[6]
        values[7] = result[7]
        values[15] = result[8]
    }
}

private fun toRadians(degrees: Double): Double = degrees / 180.0 * PI

internal data class CardContext(
    val depth: Int,
    val position: Position,
    val environment: CardAnimationEnvironment,
) {
    internal enum class Position {
        PreEntered,
        Inside,
        Accepted,
        Declined,
    }
}

internal data class CardAnimationEnvironment(
    val maxWidth: Float,
    val maxHeight: Float,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
)

internal interface StabilizationCapability {
    suspend fun runStabilization()
}

internal abstract class StabilizedItemAnimation<Context> :
    BaseItemAnimation<Context>(),
    StabilizationCapability {
    private val entries = mutableListOf<StabilizationEntry<*>>()

    fun <T> AnimatedValue<T>.stabilize(
        spec: AnimationSpec<T>,
        targetAnimation: MutableAnimatedValue<T>,
        transform: (T) -> T = { it },
    ): AnimatedValue<T> {
        val entry = StabilizationEntry({ transform(value) }, spec, targetAnimation)
        entries.add(entry)
        return entry
    }

    override suspend fun runStabilization() = coroutineScope {
        entries.fastForEach { launch { it.run() } }
    }

    private class StabilizationEntry<T>(
        val transform: () -> T,
        val spec: AnimationSpec<T>,
        val stabilizedAnimation: MutableAnimatedValue<T>,
    ) : AnimatedValue<T> by stabilizedAnimation {
        suspend fun run() = coroutineScope {
            snapshotFlow(transform).collect {
                this@coroutineScope.launch { stabilizedAnimation.animateTo(it, spec) }
            }
        }
    }
}

internal suspend fun ItemAnimation<*>.runStabilization() {
    getAndSelectCapability<StabilizationCapability>()?.runStabilization()
}
