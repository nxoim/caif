package com.nxoim.caif.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.swipeable.SwipeConstraint
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.math.cos
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SwipeConstraintClassificationBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val testVectors = List(1000) { index ->
        val rad = (index * 0.36f) * (Math.PI.toFloat() / 180f)
        val distance = 10f + (index % 50).toFloat()
        Offset(x = cos(rad) * distance, y = sin(rad) * distance)
    }

    @Test
    fun a_classifyAll8WayLtr() = benchmarkRule.measureRepeated {
        val constraint = SwipeConstraint.all(LayoutDirection.Ltr)
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
    }

    @Test
    fun b_classifyAll8WayRtl() = benchmarkRule.measureRepeated {
        val constraint = SwipeConstraint.all(LayoutDirection.Rtl)
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
    }

    @Test
    fun c_classifyFourWay() = benchmarkRule.measureRepeated {
        val constraint = SwipeConstraint.fourWay(LayoutDirection.Ltr)
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
    }

    @Test
    fun d_classifySingleDirectionTolerance() = benchmarkRule.measureRepeated {
        val constraint = SwipeConstraint.start(LayoutDirection.Ltr)
        var matches = 0
        for (vector in testVectors) {
            if (constraint.classify(vector) != null) {
                matches++
            }
        }
    }
}
