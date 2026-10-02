package com.nxoim.caif.benchmark

import androidx.benchmark.BlackHole
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.prefabs.stack.AffectedItemsPolicy
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StackPolicyBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val stackItems = (0 until 100).map { "screen-$it" }
    private val activeKeys = stackItems.takeLast(20).toSet()
    private val keysMap = stackItems.associateWith { it }
    private val currentContexts = stackItems.associateWith { 0 }
    private val targetContexts = stackItems.associateWith { 0 }
    private val previousContexts = stackItems.associateWith { 0 }
    private val visibleKeys = stackItems.takeLast(3).toSet()
    private val isVisible: (String, Int) -> Boolean = { key, _ -> key in visibleKeys }
    private val byStackIndex = RenderOrderStrategy.byStackIndex<String>()
    private val insertionOrder = RenderOrderStrategy.insertionOrder<String>()
    private val fromTop = AffectedItemsPolicy.fromTop<String, String, Int>()

    @Test
    fun renderOrderByStackIndex() = benchmarkRule.measureRepeated {
        BlackHole.consume(byStackIndex.order(activeKeys, stackItems))
    }

    @Test
    fun renderOrderInsertion() = benchmarkRule.measureRepeated {
        BlackHole.consume(insertionOrder.order(activeKeys, stackItems))
    }

    @Test
    fun affectedItemsOcclusionPruning() = benchmarkRule.measureRepeated {
        BlackHole.consume(fromTop.selectAffectedItems(
            stack = stackItems,
            keys = keysMap,
            isVisible = isVisible,
            currentContexts = currentContexts,
            targetContexts = targetContexts,
            previousContexts = previousContexts,
            maxAffected = 10,
            minAffected = 1,
        ))
    }
}
