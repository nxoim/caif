package com.nxoim.caif.benchmark

import androidx.benchmark.BlackHole
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.core.base.AnimatedDp
import com.nxoim.caif.core.base.AnimatedFloat
import com.nxoim.caif.core.base.AnimatedIntOffset
import com.nxoim.caif.core.base.AnimatedOffset
import com.nxoim.caif.core.base.GenericMutableAnimatedValue
import com.nxoim.caif.core.base.mutableColorStateOf
import com.nxoim.caif.core.base.mutableDpStateOf
import com.nxoim.caif.core.base.mutableIntOffsetStateOf
import com.nxoim.caif.core.base.mutableOffsetStateOf
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OptimizedStateComparisonBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun rawOffset_optimized() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableOffsetStateOf(Offset.Zero) }
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = Offset(i.toFloat(), (i * 2f))
            acc += state.value.x
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawOffset_standard() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableStateOf(Offset.Zero) }
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = Offset(i.toFloat(), (i * 2f))
            acc += state.value.x
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawIntOffset_optimized() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableIntOffsetStateOf(IntOffset.Zero) }
        var acc = 0
        for (i in 0 until 5000) {
            state.value = IntOffset(i, i * 2)
            acc += state.value.x
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawIntOffset_standard() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableStateOf(IntOffset.Zero) }
        var acc = 0
        for (i in 0 until 5000) {
            state.value = IntOffset(i, i * 2)
            acc += state.value.x
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawFloat_optimized() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableFloatStateOf(0f) }
        var acc = 0f
        for (i in 0 until 5000) {
            state.floatValue = i.toFloat()
            acc += state.floatValue
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawFloat_standard() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableStateOf(0f) }
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = i.toFloat()
            acc += state.value
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawDp_optimized() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableDpStateOf(0.dp) }
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = i.dp
            acc += state.value.value
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawDp_standard() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableStateOf(0.dp) }
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = i.dp
            acc += state.value.value
        }
        BlackHole.consume(acc)
    }

    @Test
    fun rawColor_optimized() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableColorStateOf(Color.Black) }
        var acc = 0UL
        for (i in 0 until 5000) {
            state.value = Color(0xFF000000UL or i.toULong())
            acc = acc xor state.value.value
        }
        BlackHole.consume(acc.toLong())
    }

    @Test
    fun rawColor_standard() = benchmarkRule.measureRepeated {
        val state = runWithMeasurementDisabled { mutableStateOf(Color.Black) }
        var acc = 0UL
        for (i in 0 until 5000) {
            state.value = Color(0xFF000000UL or i.toULong())
            acc = acc xor state.value.value
        }
        BlackHole.consume(acc.toLong())
    }

    @Test
    fun idleAnimatedOffsetWrites_optimized() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled { AnimatedOffset(Offset.Zero) }
        for (i in 0 until 500) {
            animated.value = Offset(i.toFloat(), (i * 2).toFloat())
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedOffsetWrites_standard() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled {
            GenericMutableAnimatedValue(Offset.VectorConverter, Offset.Zero, { Offset.Zero })
        }
        for (i in 0 until 500) {
            animated.value = Offset(i.toFloat(), (i * 2).toFloat())
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedFloatWrites_optimized() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled { AnimatedFloat(0f) }
        for (i in 0 until 500) {
            animated.value = i.toFloat()
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedFloatWrites_standard() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled {
            GenericMutableAnimatedValue(Float.VectorConverter, 0f, { 0f })
        }
        for (i in 0 until 500) {
            animated.value = i.toFloat()
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedIntOffsetWrites_optimized() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled { AnimatedIntOffset(IntOffset.Zero) }
        for (i in 0 until 500) {
            animated.value = IntOffset(i, i * 2)
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedIntOffsetWrites_standard() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled {
            GenericMutableAnimatedValue(IntOffset.VectorConverter, IntOffset.Zero, { IntOffset.Zero })
        }
        for (i in 0 until 500) {
            animated.value = IntOffset(i, i * 2)
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedDpWrites_optimized() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled { AnimatedDp(0.dp) }
        for (i in 0 until 500) {
            animated.value = i.dp
        }
        BlackHole.consume(animated.value)
    }

    @Test
    fun idleAnimatedDpWrites_standard() = benchmarkRule.measureRepeated {
        val animated = runWithMeasurementDisabled {
            GenericMutableAnimatedValue(Dp.VectorConverter, 0.dp, { 0.dp })
        }
        for (i in 0 until 500) {
            animated.value = i.dp
        }
        BlackHole.consume(animated.value)
    }
}
