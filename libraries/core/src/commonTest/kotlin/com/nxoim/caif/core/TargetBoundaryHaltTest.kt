package com.nxoim.caif.core

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import com.nxoim.caif.core.base.AnimatedFloat
import com.nxoim.caif.core.base.AnimatedOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TargetBoundaryHaltTest {

    @Test
    fun givenBouncySpring_whenStopOnTargetReachedTrue_thenHaltsAtBoundaryWithoutOvershooting() = runTest {
        val clock = BroadcastFrameClock()
        withContext(clock) {
            val anim = AnimatedFloat(0f)

            // spring that naturally overshoots 100f
            val bouncySpec = spring<Float>(dampingRatio = 0.2f, stiffness = Spring.StiffnessMedium)

            var animationCompletedNaturally = false
            val job = launch {
                anim.animateTo(
                    target = 100f,
                    spec = bouncySpec,
                    stopOnTargetReached = true
                )
                animationCompletedNaturally = true
            }

            var timeNanos = 0L
            for (frame in 1..60) {
                timeNanos += 16_666_667L
                runCurrent()
                clock.sendFrame(timeNanos)
                runCurrent()
                if (job.isCompleted) break
            }

            assertEquals(100f, anim.value)
            assertEquals(0f, anim.velocity)
            assertTrue(animationCompletedNaturally, "TargetReachedCancellation must complete without throwing to caller")
        }
    }

    @Test
    fun givenAnimatedOffset_whenTargetReached_thenHaltsCleanlyOnBothAxes() = runTest {
        val clock = BroadcastFrameClock()
        withContext(clock) {
            val offsetAnim = AnimatedOffset(Offset.Zero)

            val job = launch {
                offsetAnim.animateTo(
                    target = Offset(50f, 50f),
                    spec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium),
                    stopOnTargetReached = true
                )
            }

            var timeNanos = 0L
            for (frame in 1..60) {
                timeNanos += 16_666_667L
                runCurrent()
                clock.sendFrame(timeNanos)
                runCurrent()
                if (job.isCompleted) break
            }

            assertEquals(Offset(50f, 50f), offsetAnim.value)
            assertEquals(Offset.Zero, offsetAnim.velocity)
        }
    }
}
