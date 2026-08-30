@file:Suppress("NOTHING_TO_INLINE")

package com.nxoim.caif.core.base

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

fun mutableDpStateOf(value: Dp): MutableDpState = MutableDpStateImpl(value)

@Stable
interface DpState : State<Dp> {
    override val value: Dp
}

@Stable
interface MutableDpState : DpState, MutableState<Dp> {
    override var value: Dp
}

@JvmInline
private value class MutableDpStateImpl(
    private val inner: MutableFloatState
) :  MutableDpState {

    constructor(initial: Dp) : this(mutableFloatStateOf(initial.value))

    override var value: Dp
        get() = Dp(inner.floatValue)
        set(v) {
            inner.floatValue = v.value
        }

    override operator fun component1(): Dp = value

    override operator fun component2(): (Dp) -> Unit = { value = it }
}

fun mutableOffsetStateOf(value: Offset): MutableOffsetState = MutableOffsetStateImpl(value)
@JvmName("mutableOffsetStateOfXy")
fun mutableOffsetStateOf(x: Float, y: Float): MutableOffsetState = MutableOffsetStateImpl(Offset(x, y))

@Stable
interface OffsetState : State<Offset> {
    override val value: Offset
}

@Stable
interface MutableOffsetState : OffsetState, MutableState<Offset> {
    override var value: Offset
}

@JvmInline
private value class MutableOffsetStateImpl(
    private val inner: MutableLongState
) : MutableOffsetState {

    constructor(initial: Offset) : this(mutableLongStateOf(initial.packedValue))

    override var value: Offset
        get() = Offset(inner.longValue)
        set(v) {
            inner.longValue = v.packedValue
        }

    override operator fun component1(): Offset = value

    override operator fun component2(): (Offset) -> Unit = { value = it }
}

fun mutableSizeStateOf(value: Size): MutableSizeState = MutableSizeStateImpl(value)
@JvmName("mutableSizeStateOfWh")
fun mutableSizeStateOf(width: Float, height: Float): MutableSizeState = MutableSizeStateImpl(Size(width, height))

@Stable
interface SizeState : State<Size> {
    override val value: Size
}

@Stable
interface MutableSizeState : SizeState, MutableState<Size> {
    override var value: Size
}


@JvmInline
private value class MutableSizeStateImpl(
    private val inner: MutableLongState
) : MutableSizeState {

    constructor(initial: Size) : this(mutableLongStateOf(initial.packedValue))

    override var value: Size
        get() = Size(inner.longValue)
        set(v) {
            inner.longValue = v.packedValue
        }

    override operator fun component1(): Size = value

    override operator fun component2(): (Size) -> Unit = { value = it }
}

fun mutableIntOffsetStateOf(value: IntOffset): MutableIntOffsetState = MutableIntOffsetStateImpl(value)
@JvmName("mutableIntOffsetStateOfXy")
fun mutableIntOffsetStateOf(x: Int, y: Int): MutableIntOffsetState = MutableIntOffsetStateImpl(IntOffset(x, y))

@Stable
interface IntOffsetState : State<IntOffset> {
    override val value: IntOffset
}

@Stable
interface MutableIntOffsetState : IntOffsetState, MutableState<IntOffset> {
    override var value: IntOffset
}

@JvmInline
private value class MutableIntOffsetStateImpl(
    private val inner: MutableLongState
) : MutableIntOffsetState {

    constructor(initial: IntOffset) : this(mutableLongStateOf(initial.packedValue))

    override var value: IntOffset
        get() = IntOffset(inner.longValue)
        set(v) {
            inner.longValue = v.packedValue
        }

    override operator fun component1(): IntOffset = value

    override operator fun component2(): (IntOffset) -> Unit = { value = it }
}

fun mutableDpOffsetStateOf(value: DpOffset): MutableDpOffsetState = MutableDpOffsetStateImpl(value)
@JvmName("mutableDpOffsetStateOfXy")
fun mutableDpOffsetStateOf(x: Dp, y: Dp): MutableDpOffsetState = MutableDpOffsetStateImpl(DpOffset(x, y))

@Stable
interface DpOffsetState : State<DpOffset> {
    override val value: DpOffset
}

@Stable
interface MutableDpOffsetState : DpOffsetState, MutableState<DpOffset> {
    override var value: DpOffset
}

@JvmInline
private value class MutableDpOffsetStateImpl(
    private val inner: MutableLongState
) : MutableDpOffsetState {

    constructor(initial: DpOffset) : this(mutableLongStateOf(initial.packedValue))

    override var value: DpOffset
        get() = DpOffset(inner.longValue)
        set(v) {
            inner.longValue = v.packedValue
        }

    override operator fun component1(): DpOffset = value

    override operator fun component2(): (DpOffset) -> Unit = { value = it }
}

fun mutableColorStateOf(value: Color): MutableColorState = MutableColorStateImpl(value)
@JvmName("mutableColorStateOfRgba")
fun mutableColorStateOf(red: Int, green: Int, blue: Int, alpha: Int = 255): MutableColorState =
    MutableColorStateImpl(Color(red, green, blue, alpha))
@JvmName("mutableColorStateOfLong")
fun mutableColorStateOf(value: Long): MutableColorState = MutableColorStateImpl(Color(value))

@Stable
interface ColorState : State<Color> {
    override val value: Color
}

@Stable
interface MutableColorState : ColorState, MutableState<Color> {
    override var value: Color
}

@JvmInline
private value class MutableColorStateImpl(
    private val inner: MutableLongState
) : MutableColorState {

    constructor(initial: Color) : this(mutableLongStateOf(initial.value.toLong()))

    override var value: Color
        get() = Color(inner.longValue.toULong())
        set(v) {
            inner.longValue = v.value.toLong()
        }

    override operator fun component1(): Color = value

    override operator fun component2(): (Color) -> Unit = { value = it }
}

fun mutableVelocityStateOf(value: Velocity): MutableVelocityState = MutableVelocityStateImpl(value)
@JvmName("mutableVelocityStateOfXy")
fun mutableVelocityStateOf(x: Float, y: Float): MutableVelocityState = MutableVelocityStateImpl(Velocity(x, y))

@Stable
interface VelocityState : State<Velocity> {
    override val value: Velocity
}

@Stable
interface MutableVelocityState : VelocityState, MutableState<Velocity> {
    override var value: Velocity
}

@JvmInline
private value class MutableVelocityStateImpl(
    private val inner: MutableLongState
) : MutableVelocityState {

    constructor(initial: Velocity) : this(mutableLongStateOf(packFloats(initial.x, initial.y)))

    override var value: Velocity
        get() = Velocity(unpackFloat1(inner.longValue), unpackFloat2(inner.longValue))
        set(v) {
            inner.longValue = packFloats(v.x, v.y)
        }

    override operator fun component1(): Velocity = value

    override operator fun component2(): (Velocity) -> Unit = { value = it }
}

fun mutableIntSizeStateOf(value: IntSize): MutableIntSizeState = MutableIntSizeStateImpl(value)
@JvmName("mutableIntSizeStateOfWh")
fun mutableIntSizeStateOf(width: Int, height: Int): MutableIntSizeState = MutableIntSizeStateImpl(IntSize(width, height))

@Stable
interface IntSizeState : State<IntSize> {
    override val value: IntSize
}

inline operator fun IntSizeState.getValue(thisObj: Any?, property: KProperty<*>): IntSize = value

@Stable
interface MutableIntSizeState : IntSizeState, MutableState<IntSize> {
    override var value: IntSize
}

@JvmInline
private value class MutableIntSizeStateImpl(
    private val inner: MutableLongState
) : MutableIntSizeState {
    constructor(initial: IntSize) : this(mutableLongStateOf(initial.packedValue))

    override var value: IntSize
        get() = IntSize(
            unpackInt1(inner.longValue),
            unpackInt2(inner.longValue)
        )
        set(v) {
            inner.longValue = v.packedValue
        }

    override operator fun component1(): IntSize = value

    override operator fun component2(): (IntSize) -> Unit = { value = it }
}


@Stable
interface DpSizeState : State<DpSize> {
    override val value: DpSize
}

@Stable
interface MutableDpSizeState : DpSizeState, MutableState<DpSize> {
    override var value: DpSize
}

@JvmInline
private value class MutableDpSizeStateImpl(
    private val inner: MutableLongState
) : MutableDpSizeState {
    constructor(initial: DpSize) : this(mutableLongStateOf(packFloats(initial.width.value, initial.height.value)))

    override var value: DpSize
        get() = DpSize(
            unpackFloat1(inner.longValue).dp,
            unpackFloat2(inner.longValue).dp
        )
        set(v) {
            inner.longValue = packFloats(v.width.value, v.height.value)
        }

    override operator fun component1(): DpSize = value

    override operator fun component2(): (DpSize) -> Unit = { value = it }
}

fun mutableDpSizeStateOf(value: DpSize): MutableDpSizeState = MutableDpSizeStateImpl(value)
@JvmName("mutableDpSizeStateOfWh")
fun mutableDpSizeStateOf(width: Dp, height: Dp): MutableDpSizeState = MutableDpSizeStateImpl(DpSize(width, height))