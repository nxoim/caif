package com.nxoim.caif.benchmark

import androidx.benchmark.BlackHole
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.swipeable.SwipeConstraint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.cos
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class SwipeConstraintClassificationBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val testVectors = List(1000) { index ->
        val rad = (index * 0.36f) * (Math.PI.toFloat() / 180f)
        val distance = 10f + (index % 50).toFloat()
        Offset(x = cos(rad) * distance, y = sin(rad) * distance)
    }

    @Test
    fun classifyAll8WayLtr() = benchmarkRule.measureRepeated {
        val constraint = runWithMeasurementDisabled { SwipeConstraint.all(LayoutDirection.Ltr) }
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
        BlackHole.consume(matches)
    }

    @Test
    fun classifyAll8WayRtl() = benchmarkRule.measureRepeated {
        val constraint = runWithMeasurementDisabled { SwipeConstraint.all(LayoutDirection.Rtl) }
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
        BlackHole.consume(matches)
    }

    @Test
    fun classifyFourWay() = benchmarkRule.measureRepeated {
        val constraint = runWithMeasurementDisabled { SwipeConstraint.fourWay(LayoutDirection.Ltr) }
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
        BlackHole.consume(matches)
    }

    @Test
    fun classifySingleDirectionTolerance() = benchmarkRule.measureRepeated {
        val constraint = runWithMeasurementDisabled { SwipeConstraint.start(LayoutDirection.Ltr) }
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
        BlackHole.consume(matches)
    }
}
