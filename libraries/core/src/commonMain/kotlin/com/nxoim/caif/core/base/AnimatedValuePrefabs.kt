package com.nxoim.caif.core.base

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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


@Stable
interface MutableFloatAnimatedValue : MutableAnimatedValue<Float> {
    override var value: Float
    override val velocity: Float
}

@Stable
interface MutableIntAnimatedValue : MutableAnimatedValue<Int> {
    override var value: Int
    override val velocity: Int
}

@Stable
interface MutableDpAnimatedValue : MutableAnimatedValue<Dp> {
    override var value: Dp
    override val velocity: Dp
}

@Stable
interface MutableOffsetAnimatedValue : MutableAnimatedValue<Offset> {
    override var value: Offset
    override val velocity: Offset
}

@Stable
interface MutableIntOffsetAnimatedValue : MutableAnimatedValue<IntOffset> {
    override var value: IntOffset
    override val velocity: IntOffset
}

@Stable
interface MutableSizeAnimatedValue : MutableAnimatedValue<Size> {
    override var value: Size
    override val velocity: Size
}

@Stable
interface MutableIntSizeAnimatedValue : MutableAnimatedValue<IntSize> {
    override var value: IntSize
    override val velocity: IntSize
}

@Stable
interface MutableDpOffsetAnimatedValue : MutableAnimatedValue<DpOffset> {
    override var value: DpOffset
    override val velocity: DpOffset
}

@Stable
interface MutableDpSizeAnimatedValue : MutableAnimatedValue<DpSize> {
    override var value: DpSize
    override val velocity: DpSize
}

@Stable
interface MutableColorAnimatedValue : MutableAnimatedValue<Color> {
    override var value: Color
    override val velocity: Color
}

class AnimatedFloat(
    initialValue: Float = 0f,
    label: String = "AnimatedFloat",
) : MutableFloatAnimatedValue, AbstractMutableAnimatedValue<Float, AnimationVector1D>(
    converter = Float.VectorConverter,
    zeroVelocity = 0f,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableFloatStateOf(initialValue)
    private val velocityState = mutableFloatStateOf(0f)

    override var value: Float
        get() = if (snapped) snapState.floatValue else animatable.value
        set(newValue) {
            beginDirectWrite()

            velocityState.floatValue = 0f
            snapState.floatValue = newValue

            finishDirectWrite()
        }

    override val velocity: Float
        get() = velocityState.floatValue

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.floatValue)
    }

    override fun updateVelocityState(value: Float) {
        velocityState.floatValue = value
    }

    override fun clearVelocityState() {
        velocityState.floatValue = 0f
    }

    override fun hasCrossedTarget(
        current: Float,
        target: Float,
        start: Float,
    ): Boolean = (current - target) * (start - target) <= 0f
}

class AnimatedInt(
    initialValue: Int = 0,
    label: String = "AnimatedInt",
) : MutableIntAnimatedValue, AbstractMutableAnimatedValue<Int, AnimationVector1D>(
    converter = Int.VectorConverter,
    zeroVelocity = 0,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableIntStateOf(initialValue)
    private val velocityState = mutableIntStateOf(0)

    override var value: Int
        get() = if (snapped) snapState.intValue else animatable.value
        set(newValue) {
            beginDirectWrite()

            velocityState.intValue = 0
            snapState.intValue = newValue

            finishDirectWrite()
        }

    override val velocity: Int
        get() = velocityState.intValue

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.intValue)
    }

    override fun updateVelocityState(value: Int) {
        velocityState.intValue = value
    }

    override fun clearVelocityState() {
        velocityState.intValue = 0
    }

    override fun hasCrossedTarget(
        current: Int,
        target: Int,
        start: Int,
    ): Boolean = (current - target).toLong() * (start - target).toLong() <= 0L
}


class AnimatedDp(
    initialValue: Dp = 0.dp,
    label: String = "AnimatedDp",
) : AbstractMutableAnimatedValue<Dp, AnimationVector1D>(
    converter = Dp.VectorConverter,
    zeroVelocity = 0.dp,
    initialValue = initialValue,
    label = label,
), MutableDpAnimatedValue {
    private val snapState = mutableDpStateOf(initialValue)
    private val velocityState = mutableDpStateOf(0.dp)

    override var value: Dp
        get() = if (snapped) snapState.value else animatable.value
        set(newValue) {
            beginDirectWrite()


            velocityState.value = 0.dp
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: Dp
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: Dp) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = 0.dp
    }

    override fun hasCrossedTarget(
        current: Dp,
        target: Dp,
        start: Dp,
    ): Boolean = (current - target) * (start - target) <= 0f
}


class AnimatedOffset(
    initialValue: Offset = Offset.Zero,
    label: String = "AnimatedOffset",
) : MutableOffsetAnimatedValue, AbstractMutableAnimatedValue<Offset, AnimationVector2D>(
    converter = Offset.VectorConverter,
    zeroVelocity = Offset.Zero,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableOffsetStateOf(initialValue)
    private val velocityState = mutableOffsetStateOf(Offset.Zero)

    override var value: Offset
        get() = if (snapped) snapState.value else animatable.value
        set(newValue) {
            beginDirectWrite()

            velocityState.value = Offset.Zero
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: Offset
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: Offset) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = Offset.Zero
    }

    override fun hasCrossedTarget(
        current: Offset,
        target: Offset,
        start: Offset,
    ): Boolean =
        (current.x - target.x) * (start.x - target.x) <= 0f &&
                (current.y - target.y) * (start.y - target.y) <= 0f
}


class AnimatedIntOffset(
    initialValue: IntOffset = IntOffset.Zero,
    label: String = "AnimatedIntOffset",
) : MutableIntOffsetAnimatedValue, AbstractMutableAnimatedValue<IntOffset, AnimationVector2D>(
    converter = IntOffset.VectorConverter,
    zeroVelocity = IntOffset.Zero,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableIntOffsetStateOf(initialValue)
    private val velocityState = mutableIntOffsetStateOf(IntOffset.Zero)

    override var value: IntOffset
        get() = if (snapped) snapState.value else animatable.value
        set(newValue) {
            beginDirectWrite()

            velocityState.value = IntOffset.Zero
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: IntOffset
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: IntOffset) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = IntOffset.Zero
    }

    override fun hasCrossedTarget(
        current: IntOffset,
        target: IntOffset,
        start: IntOffset,
    ): Boolean = (current.x - target.x).toLong() *
            (start.x - target.x).toLong() <= 0L &&
            (current.y - target.y).toLong() *
            (start.y - target.y).toLong() <= 0L
}


class AnimatedSize(
    initialValue: Size = Size.Zero,
    label: String = "AnimatedSize",
) : MutableSizeAnimatedValue, AbstractMutableAnimatedValue<Size, AnimationVector2D>(
    converter = Size.VectorConverter,
    zeroVelocity = Size.Zero,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableSizeStateOf(initialValue)
    private val velocityState = mutableSizeStateOf(Size.Zero)

    override var value: Size
        get() = if (snapped) snapState.value else animatable.value
        set(newValue) {
            beginDirectWrite()

            velocityState.value = Size.Zero
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: Size
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: Size) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = Size.Zero
    }

    override fun hasCrossedTarget(
        current: Size,
        target: Size,
        start: Size,
    ): Boolean = (current.width - target.width) *
            (start.width - target.width) <= 0f &&
            (current.height - target.height) *
            (start.height - target.height) <= 0f
}


class AnimatedIntSize(
    initialValue: IntSize = IntSize.Zero,
    label: String = "AnimatedIntSize",
) : MutableIntSizeAnimatedValue, AbstractMutableAnimatedValue<IntSize, AnimationVector2D>(
    converter = IntSize.VectorConverter,
    zeroVelocity = IntSize.Zero,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableIntSizeStateOf(initialValue)
    private val velocityState = mutableIntSizeStateOf(IntSize.Zero)

    override var value: IntSize
        get() = if (snapped) snapState.value else animatable.value
        set(newValue) {
            beginDirectWrite()

            velocityState.value = IntSize.Zero
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: IntSize
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: IntSize) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = IntSize.Zero
    }

    override fun hasCrossedTarget(
        current: IntSize,
        target: IntSize,
        start: IntSize,
    ): Boolean = (current.width - target.width).toLong() *
            (start.width - target.width).toLong() <= 0L &&
            (current.height - target.height).toLong() *
            (start.height - target.height).toLong() <= 0L
}


class AnimatedDpOffset(
    initialValue: DpOffset = DpOffset.Zero,
    label: String = "AnimatedDpOffset",
) : MutableDpOffsetAnimatedValue, AbstractMutableAnimatedValue<DpOffset, AnimationVector2D>(
    converter = DpOffset.VectorConverter,
    zeroVelocity = DpOffset.Zero,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableDpOffsetStateOf(initialValue)
    private val velocityState = mutableDpOffsetStateOf(DpOffset.Zero)

    override var value: DpOffset
        get() = if (snapped) snapState.value else animatable.value

        set(newValue) {
            beginDirectWrite()

            velocityState.value = DpOffset.Zero
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: DpOffset
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: DpOffset) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = DpOffset.Zero
    }

    override fun hasCrossedTarget(
        current: DpOffset,
        target: DpOffset,
        start: DpOffset,
    ): Boolean = (current.x - target.x) *
                (start.x - target.x) <= 0f &&
                (current.y - target.y) *
                (start.y - target.y) <= 0f
}


class AnimatedDpSize(
    initialValue: DpSize = DpSize.Zero,
    label: String = "AnimatedDpSize",
) : MutableDpSizeAnimatedValue, AbstractMutableAnimatedValue<DpSize, AnimationVector2D>(
    converter = DpSizeVectorConverter,
    zeroVelocity = DpSize.Zero,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableDpSizeStateOf(initialValue)
    private val velocityState = mutableDpSizeStateOf(DpSize.Zero)

    override var value: DpSize
        get() = if (snapped) snapState.value else animatable.value

        set(newValue) {
            beginDirectWrite()

            velocityState.value = DpSize.Zero
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: DpSize
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: DpSize) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = DpSize.Zero
    }

    override fun hasCrossedTarget(
        current: DpSize,
        target: DpSize,
        start: DpSize,
    ): Boolean = (current.width - target.width) *
                (start.width - target.width) <= 0f &&
                (current.height - target.height) *
                (start.height - target.height) <= 0f
}


class AnimatedColor(
    initialValue: Color = Color.Transparent,
    colorSpace: ColorSpace = ColorSpaces.Srgb,
    label: String = "AnimatedColor",
) : MutableColorAnimatedValue, AbstractMutableAnimatedValue<Color, AnimationVector4D>(
    converter = Color.VectorConverter(colorSpace),
    zeroVelocity = Color.Transparent,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableColorStateOf(initialValue)
    private val velocityState = mutableColorStateOf(Color.Transparent)

    override var value: Color
        get() = if (snapped) snapState.value else animatable.value

        set(newValue) {
            beginDirectWrite()

            velocityState.value = Color.Transparent
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: Color
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: Color) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = Color.Transparent
    }
}

private val DpSizeVectorConverter =
    TwoWayConverter<DpSize, AnimationVector2D>(
        convertToVector = {
            AnimationVector2D(
                it.width.value,
                it.height.value,
            )
        },
        convertFromVector = {
            DpSize(
                it.v1.dp,
                it.v2.dp,
            )
        },
    )

private operator fun Dp.times(other: Dp): Float =
    value * other.value
