package com.nxoim.caif.benchmark

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
    fun a1_rawOffset_optimized() = benchmarkRule.measureRepeated {
        val state = mutableOffsetStateOf(Offset.Zero)
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = Offset(i.toFloat(), (i * 2).toFloat())
            acc += state.value.x
        }
    }

    @Test
    fun a2_rawOffset_standard() = benchmarkRule.measureRepeated {
        val state = mutableStateOf(Offset.Zero)
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = Offset(i.toFloat(), (i * 2).toFloat())
            acc += state.value.x
        }
    }

    @Test
    fun b1_rawIntOffset_optimized() = benchmarkRule.measureRepeated {
        val state = mutableIntOffsetStateOf(IntOffset.Zero)
        var acc = 0
        for (i in 0 until 5000) {
            state.value = IntOffset(i, i * 2)
            acc += state.value.x
        }
    }

    @Test
    fun b2_rawIntOffset_standard() = benchmarkRule.measureRepeated {
        val state = mutableStateOf(IntOffset.Zero)
        var acc = 0
        for (i in 0 until 5000) {
            state.value = IntOffset(i, i * 2)
            acc += state.value.x
        }
    }

    @Test
    fun c1_rawFloat_optimized() = benchmarkRule.measureRepeated {
        val state = mutableFloatStateOf(0f)
        var acc = 0f
        for (i in 0 until 5000) {
            state.floatValue = i.toFloat()
            acc += state.floatValue
        }
    }

    @Test
    fun c2_rawFloat_standard() = benchmarkRule.measureRepeated {
        val state = mutableStateOf(0f)
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = i.toFloat()
            acc += state.value
        }
    }

    @Test
    fun d1_rawDp_optimized() = benchmarkRule.measureRepeated {
        val state = mutableDpStateOf(0.dp)
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = i.dp
            acc += state.value.value
        }
    }

    @Test
    fun d2_rawDp_standard() = benchmarkRule.measureRepeated {
        val state = mutableStateOf(0.dp)
        var acc = 0f
        for (i in 0 until 5000) {
            state.value = i.dp
            acc += state.value.value
        }
    }

    @Test
    fun e1_rawColor_optimized() = benchmarkRule.measureRepeated {
        val state = mutableColorStateOf(Color.Black)
        var acc = 0UL
        for (i in 0 until 5000) {
            state.value = Color(0xFF000000UL or i.toULong())
            acc = acc xor state.value.value
        }
    }

    @Test
    fun e2_rawColor_standard() = benchmarkRule.measureRepeated {
        val state = mutableStateOf(Color.Black)
        var acc = 0UL
        for (i in 0 until 5000) {
            state.value = Color(0xFF000000UL or i.toULong())
            acc = acc xor state.value.value
        }
    }

    // --- AnimatedValue Interruption Snaps (500 iterations) ---

    @Test
    fun f1_animatedOffset_optimized() = benchmarkRule.measureRepeated {
        val animated = AnimatedOffset(Offset.Zero)
        for (i in 0 until 500) {
            animated.value = Offset(i.toFloat(), (i * 2).toFloat())
        }
    }

    @Test
    fun f2_animatedOffset_standard() = benchmarkRule.measureRepeated {
        val animated = GenericMutableAnimatedValue(Offset.VectorConverter, Offset.Zero, { Offset.Zero })
        for (i in 0 until 500) {
            animated.value = Offset(i.toFloat(), (i * 2).toFloat())
        }
    }

    @Test
    fun g1_animatedFloat_optimized() = benchmarkRule.measureRepeated {
        val animated = AnimatedFloat(0f)
        for (i in 0 until 500) {
            animated.value = i.toFloat()
        }
    }

    @Test
    fun g2_animatedFloat_standard() = benchmarkRule.measureRepeated {
        val animated = GenericMutableAnimatedValue(Float.VectorConverter, 0f, { 0f })
        for (i in 0 until 500) {
            animated.value = i.toFloat()
        }
    }

    @Test
    fun h1_animatedIntOffset_optimized() = benchmarkRule.measureRepeated {
        val animated = AnimatedIntOffset(IntOffset.Zero)
        for (i in 0 until 500) {
            animated.value = IntOffset(i, i * 2)
        }
    }

    @Test
    fun h2_animatedIntOffset_standard() = benchmarkRule.measureRepeated {
        val animated = GenericMutableAnimatedValue(IntOffset.VectorConverter, IntOffset.Zero, { IntOffset.Zero })
        for (i in 0 until 500) {
            animated.value = IntOffset(i, i * 2)
        }
    }

    @Test
    fun i1_animatedDp_optimized() = benchmarkRule.measureRepeated {
        val animated = AnimatedDp(0.dp)
        for (i in 0 until 500) {
            animated.value = i.dp
        }
    }

    @Test
    fun i2_animatedDp_standard() = benchmarkRule.measureRepeated {
        val animated = GenericMutableAnimatedValue(Dp.VectorConverter, 0.dp, { 0.dp })
        for (i in 0 until 500) {
            animated.value = i.dp
        }
    }
}
