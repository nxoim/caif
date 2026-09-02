package com.nxoim.caif.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.prefabs.list.CollectionSwipeAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CollectionSwipeAnimatorBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val keys = (0 until 100).map { "item-$it" }

    private class MockItemAnimation : ItemAnimation<Unit> {
        override val modifier: Modifier = Modifier
        override fun reset(context: Unit) {}
        override fun willBeVisible(context: Unit): Boolean = true
        override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? = null
        override suspend fun animateTo(target: Unit) {}
    }

    private fun createAnimator(): CollectionSwipeAnimator<String, Unit> {
        return CollectionSwipeAnimator(
            settleableFactory = { MockItemAnimation() },
            contextFor = { },
            scope = testScope,
            visibleKeys = { keys },
        )
    }

    @Test
    fun a_gestureStartAndQueryCapabilities() = benchmarkRule.measureRepeated {
        val animator = createAnimator()
        for (i in 0 until 20) {
            val key = keys[i * 5]
            animator.onStart(key)
            animator.activeGestureCapabilities(Any::class)
            animator.onEnd()
        }
    }

    @Test
    fun b_gestureSelectiveRelease() = benchmarkRule.measureRepeated {
        val animator = createAnimator()
        val startKey = keys[50]
        animator.onStart(startKey)
        for (i in 0 until 50) {
            animator.releaseFromGesture(keys[i])
        }
        animator.onEnd()
    }
}
