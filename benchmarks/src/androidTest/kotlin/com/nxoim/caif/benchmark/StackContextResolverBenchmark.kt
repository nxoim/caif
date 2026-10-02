package com.nxoim.caif.benchmark

import androidx.benchmark.BlackHole
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.prefabs.stack.defaultStackContextResolver
import com.nxoim.caif.prefabs.stack.indexOf
import com.nxoim.caif.prefabs.stack.previousIndexOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StackContextResolverBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun transitionWithIndexLookups10Items() = measureTransition(10)

    @Test
    fun transitionWithIndexLookups100Items() = measureTransition(100)

    @Test
    fun transitionWithIndexLookups1000Items() = measureTransition(1000)

    private fun measureTransition(size: Int) {
        // Per-build index-cache creation remains measured.
        val previous = (0 until size).toList()
        val current = (1..size).toList()
        val resolver = defaultStackContextResolver<Pair<Int, Int?>, Int, Int>(
            contextFactory = { item -> indexOf(item) to previousIndexOf(item) },
            keyFor = { it }
        )
        benchmarkRule.measureRepeated {
            BlackHole.consume(resolver.buildContexts(
                stack = current,
                previousStack = previous,
                treatNewEnteringAsPreparing = true,
                recalculateEnteringToMoving = false,
                previousContexts = null
            ))
        }
    }
}
