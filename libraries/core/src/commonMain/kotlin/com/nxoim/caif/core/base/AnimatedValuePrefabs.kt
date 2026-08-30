package com.nxoim.caif.core.base

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

class AnimatedFloat(
    initialValue: Float = 0f,
    label: String = "AnimatedFloat",
) : AbstractMutableAnimatedValue<Float, AnimationVector1D>(
    converter = Float.VectorConverter,
    zeroVelocity = 0f,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableFloatStateOf(initialValue)

    override fun hasCrossedTarget(current: Float, target: Float, start: Float) =
        (current - target) * (start - target) <= 0f
}

class AnimatedInt(
    initialValue: Int = 0,
    label: String = "AnimatedInt",
) : AbstractMutableAnimatedValue<Int, AnimationVector1D>(
    converter = Int.VectorConverter,
    zeroVelocity = 0,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableIntStateOf(initialValue)

    override fun hasCrossedTarget(current: Int, target: Int, start: Int) =
        (current - target).toLong() * (start - target).toLong() <= 0L
}

class AnimatedOffset(
    initialValue: Offset = Offset.Zero,
    label: String = "AnimatedOffset",
) : AbstractMutableAnimatedValue<Offset, AnimationVector2D>(
    converter = Offset.VectorConverter,
    zeroVelocity = Offset.Zero,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableOffsetStateOf(initialValue)

    override fun hasCrossedTarget(current: Offset, target: Offset, start: Offset) =
        (current.x - target.x) * (start.x - target.x) <= 0f &&
                (current.y - target.y) * (start.y - target.y) <= 0f
}

class AnimatedIntOffset(
    initialValue: IntOffset = IntOffset.Zero,
    label: String = "AnimatedIntOffset",
) : AbstractMutableAnimatedValue<IntOffset, AnimationVector2D>(
    converter = IntOffset.VectorConverter,
    zeroVelocity = IntOffset.Zero,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableIntOffsetStateOf(initialValue)

    override fun hasCrossedTarget(current: IntOffset, target: IntOffset, start: IntOffset) =
        (current.x - target.x).toLong() * (start.x - target.x).toLong() <= 0L &&
                (current.y - target.y).toLong() * (start.y - target.y).toLong() <= 0L
}

class AnimatedIntSize(
    initialValue: IntSize = IntSize.Zero,
    label: String = "AnimatedIntSize",
) : AbstractMutableAnimatedValue<IntSize, AnimationVector2D>(
    converter = IntSize.VectorConverter,
    zeroVelocity = IntSize.Zero,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableIntSizeStateOf(initialValue)

    override fun hasCrossedTarget(current: IntSize, target: IntSize, start: IntSize) =
        (current.width - target.width).toLong() * (start.width - target.width).toLong() <= 0L &&
                (current.height - target.height).toLong() * (start.height - target.height).toLong() <= 0L
}

class AnimatedSize(
    initialValue: Size = Size.Zero,
    label: String = "AnimatedSize",
) : AbstractMutableAnimatedValue<Size, AnimationVector2D>(
    converter = Size.VectorConverter,
    zeroVelocity = Size.Zero,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by  mutableSizeStateOf(initialValue)

    override fun hasCrossedTarget(current: Size, target: Size, start: Size) =
        (current.width - target.width) * (start.width - target.width) <= 0f &&
                (current.height - target.height) * (start.height - target.height) <= 0f
}

class AnimatedDp(
    initialValue: Dp = 0.dp,
    label: String = "AnimatedDp",
) : AbstractMutableAnimatedValue<Dp, AnimationVector1D>(
    converter = Dp.VectorConverter,
    zeroVelocity = 0.dp,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableDpStateOf(initialValue)

    override fun hasCrossedTarget(current: Dp, target: Dp, start: Dp) =
        (current - target) * (start - target) <= 0f
}

class AnimatedDpOffset(
    initialValue: DpOffset = DpOffset.Zero,
    label: String = "AnimatedDpOffset",
) : AbstractMutableAnimatedValue<DpOffset, AnimationVector2D>(
    converter = DpOffset.VectorConverter,
    zeroVelocity = DpOffset.Zero,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by  mutableDpOffsetStateOf(initialValue)

    override fun hasCrossedTarget(current: DpOffset, target: DpOffset, start: DpOffset) =
        (current.x - target.x) * (start.x - target.x) <= 0f &&
                (current.y - target.y) * (start.y - target.y) <= 0f
}

class AnimatedColor(
    initialValue: Color = Color.Transparent,
    colorSpace: ColorSpace = ColorSpaces.Srgb,
    label: String = "AnimatedColor",
) : AbstractMutableAnimatedValue<Color, AnimationVector4D>(
    converter = Color.VectorConverter(colorSpace),
    zeroVelocity = Color.Transparent,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableColorStateOf(initialValue)
}

class AnimatedDpSize(
    initialValue: DpSize = DpSize.Zero,
    label: String = "AnimatedDpSize",
) : AbstractMutableAnimatedValue<DpSize, AnimationVector2D>(
    converter = DpSizeVectorConverter,
    zeroVelocity = DpSize.Zero,
    initialValue = initialValue,
    label = label,
) {
    override var snapValue by mutableDpSizeStateOf(initialValue)

    override fun hasCrossedTarget(current: DpSize, target: DpSize, start: DpSize) =
        (current.width - target.width) * (start.width - target.width) <= 0f &&
                (current.height - target.height) * (start.height - target.height) <= 0f
}

private val DpSizeVectorConverter = TwoWayConverter<DpSize, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.width.value, it.height.value) },
    convertFromVector = { DpSize(it.v1.dp, it.v2.dp) },
)

private operator fun Dp.times(other: Dp): Float = value * other.value