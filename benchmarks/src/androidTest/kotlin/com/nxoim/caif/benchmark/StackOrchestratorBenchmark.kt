package com.nxoim.caif.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.prefabs.stack.AffectedItemsPolicy
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class StackOrchestratorBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val stackItems = (0 until 100).map { "screen-$it" }
    private val activeKeys = stackItems.takeLast(20).toSet()
    private val keysMap = stackItems.associateWith { it }

    @Test
    fun a_renderOrderStrategyByStackIndex() = benchmarkRule.measureRepeated {
        val strategy = RenderOrderStrategy.byStackIndex<String>()
        val result = strategy.order(activeKeys, stackItems)
        check(result.isNotEmpty())
    }

    @Test
    fun b_renderOrderStrategyInsertion() = benchmarkRule.measureRepeated {
        val strategy = RenderOrderStrategy.insertionOrder<String>()
        val result = strategy.order(activeKeys, stackItems)
        check(result.isNotEmpty())
    }

    @Test
    fun c_affectedItemsPolicyOcclusionPruning() = benchmarkRule.measureRepeated {
        val policy = AffectedItemsPolicy.fromTop<String, String, Int>()
        val currentContexts = stackItems.associateWith { 0 }
        val targetContexts = stackItems.associateWith { 0 }
        val previousContexts = stackItems.associateWith { 0 }

        val affected = policy.selectAffectedItems(
            stack = stackItems,
            keys = keysMap,
            isVisible = { key, _ ->
                // Only top 3 items are visible; rest are occluded
                val index = key.removePrefix("screen-").toIntOrNull() ?: 0
                index >= 97
            },
            currentContexts = currentContexts,
            targetContexts = targetContexts,
            previousContexts = previousContexts,
            maxAffected = 10,
            minAffected = 1,
        )
        check(affected.isNotEmpty())
    }
}
