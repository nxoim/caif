package com.nxoim.caif.core

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
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
import com.nxoim.caif.core.base.AnimatedColor
import com.nxoim.caif.core.base.AnimatedDp
import com.nxoim.caif.core.base.AnimatedDpOffset
import com.nxoim.caif.core.base.AnimatedDpSize
import com.nxoim.caif.core.base.AnimatedFloat
import com.nxoim.caif.core.base.AnimatedInt
import com.nxoim.caif.core.base.AnimatedIntOffset
import com.nxoim.caif.core.base.AnimatedIntSize
import com.nxoim.caif.core.base.AnimatedOffset
import com.nxoim.caif.core.base.AnimatedSize
import com.nxoim.caif.core.base.GenericMutableAnimatedValue
import com.nxoim.caif.core.base.TargetableMutableAnimatedValue

fun <Context> AnimationBuilder<Context>.animateFloat(
    spec: Context.() -> AnimationSpec<Float> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> Float = { 0f },
): TargetableMutableAnimatedValue<Float, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedFloat(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateInt(
    spec: Context.() -> AnimationSpec<Int> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> Int = { 0 },
): TargetableMutableAnimatedValue<Int, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedInt(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateOffset(
    spec: Context.() -> AnimationSpec<Offset> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> Offset = { Offset.Zero }
): TargetableMutableAnimatedValue<Offset, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedOffset(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateSize(
    spec: Context.() -> AnimationSpec<Size> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> Size = { Size.Zero },
): TargetableMutableAnimatedValue<Size, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedSize(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateIntOffset(
    spec: Context.() -> AnimationSpec<IntOffset> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> IntOffset = { IntOffset.Zero }
): TargetableMutableAnimatedValue<IntOffset, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedIntOffset(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateIntSize(
    spec: Context.() -> AnimationSpec<IntSize> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> IntSize = { IntSize.Zero }
): TargetableMutableAnimatedValue<IntSize, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedIntSize(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateDp(
    spec: Context.() -> AnimationSpec<Dp> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> Dp = { 0.dp },
): TargetableMutableAnimatedValue<Dp, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedDp(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateDpOffset(
    spec: Context.() -> AnimationSpec<DpOffset> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> DpOffset = { DpOffset.Zero },
): TargetableMutableAnimatedValue<DpOffset, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedDpOffset(), value, spec, stopOnTargetReached)
}

fun <Context> AnimationBuilder<Context>.animateColor(
    spec: Context.() -> AnimationSpec<Color> = { spring() },
    colorSpace: ColorSpace = ColorSpaces.Srgb,
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> Color = { Color.Transparent }
): TargetableMutableAnimatedValue<Color, Context> = registerAnimation {
    TargetableMutableAnimatedValue(
        AnimatedColor(colorSpace = colorSpace),
        value,
        spec,
        stopOnTargetReached
    )
}

fun <Context> AnimationBuilder<Context>.animateDpSize(
    spec: Context.() -> AnimationSpec<DpSize> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    value: Context.() -> DpSize = { DpSize.Zero }
): TargetableMutableAnimatedValue<DpSize, Context> = registerAnimation {
    TargetableMutableAnimatedValue(AnimatedDpSize(), value, spec, stopOnTargetReached)
}

fun <Context, T, V : AnimationVector> AnimationBuilder<Context>.animate(
    converter: TwoWayConverter<T, V>,
    zeroVelocity: T,
    spec: Context.() -> AnimationSpec<T> = { spring() },
    stopOnTargetReached: (Context.() -> Boolean)? = null,
    initialValue: () -> T = { zeroVelocity },
    value: Context.() -> T
): TargetableMutableAnimatedValue<T, Context> = registerAnimation {
    TargetableMutableAnimatedValue(
        GenericMutableAnimatedValue(converter, zeroVelocity, initialValue),
        value,
        spec,
        stopOnTargetReached
    )
}