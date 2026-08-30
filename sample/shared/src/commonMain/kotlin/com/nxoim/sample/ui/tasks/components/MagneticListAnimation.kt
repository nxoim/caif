package com.nxoim.sample.ui.tasks.components

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.animateOffset
import com.nxoim.caif.prefabs.list.CollectionItemPosition
import com.nxoim.caif.prefabs.list.CollectionSwipeAnimator
import com.nxoim.caif.springs.smooth
import kotlin.math.abs
import kotlin.math.exp

internal interface MagneticSwipeCapability {
    context(density: Density)
    fun onSwipeUpdate(delta: Offset, distance: Int, snapBack: () -> Unit)

    context(density: Density)
    fun onSwipeEnd(velocity: Velocity, distance: Int)
}

context(density: Density)
internal fun CollectionSwipeAnimator<String, MagneticListAnimationContext>.dispatchSwipeUpdate(
    delta: Offset,
) = activeGestureCapabilities(MagneticSwipeCapability::class)
    .forEach { (key, capabilityAndDistance) ->
        val (capability, distance) = capabilityAndDistance
        if (distance != 0) {
            capability?.onSwipeUpdate(delta, distance) {
                releaseFromGesture(key)
            }
        }
    }

context(density: Density)
internal fun CollectionSwipeAnimator<String, MagneticListAnimationContext>.dispatchSwipeEnd(
    velocity: Velocity,
) = activeGestureCapabilities(MagneticSwipeCapability::class)
    .forEach { (_, capabilityAndDistance) ->
        val (capability, distance) = capabilityAndDistance
        if (distance != 0) capability?.onSwipeEnd(velocity, distance)
    }

internal data class MagneticListAnimationEnvironment(
    val density: Density,
)

@Composable
internal fun BoxWithConstraintsScope.rememberMagneticAnimationEnvironmentState(): State<MagneticListAnimationEnvironment> {
    val density = LocalDensity.current

    return rememberUpdatedState(
        MagneticListAnimationEnvironment(
            density = density,
        ),
    )
}

internal data class MagneticListAnimationContext(
    val position: CollectionItemPosition,
    val environment: MagneticListAnimationEnvironment,
)

internal class MagneticListItemAnimation : BaseItemAnimation<MagneticListAnimationContext>(), MagneticSwipeCapability {
    val offset = animateOffset(spec = { smooth() }) {
        Offset.Zero
    }

    override val modifier: Modifier = Modifier
        .offset { offset.value.round() }

    private fun Density.snapThreshold() = 128.dp.toPx()
    private fun effectStrength(distance: Int): Float =
        exp(-0.8f * distance) * if (distance == 0) 1f else 1.3f

    context(density: Density)
    override fun onSwipeUpdate(delta: Offset, distance: Int, snapBack: () -> Unit) {
        val factor = effectStrength(distance)
        offset.value += Offset(delta.x * factor, 0f)
        if (distance > 0 && abs(offset.value.x) > with(density) { snapThreshold() } * factor) {
            snapBack()
        }
    }

    context(density: Density)
    override fun onSwipeEnd(velocity: Velocity, distance: Int) {
        offset.prepareVelocity(
            Offset(
                x = velocity.x * effectStrength(distance),
                y = 0f,
            ),
        )
    }
}

internal fun buildMagneticListAnimation(): ItemAnimation<MagneticListAnimationContext> =
    MagneticListItemAnimation()
