package com.nxoim.caif.core

import androidx.compose.animation.core.tween
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.nxoim.caif.core.base.AnimatedFloat
import com.nxoim.caif.core.base.AnimatedInt
import com.nxoim.caif.core.base.AnimatedIntOffset
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
class AnimatedValueInterruptionTest {

    @Test
    fun givenRunningAnimation_whenValueSetSynchronously_thenAnimationIsInterruptedImmediatelyWithoutSuspending() = runTest {
        val clock = BroadcastFrameClock()
        withContext(clock) {
            val anim = AnimatedFloat(0f)

            val animationJob = launch {
                anim.animateTo(
                    target = 1000f,
                    spec = tween(durationMillis = 1000)
                )
            }

            runCurrent()
            clock.sendFrame(0L)
            runCurrent()
            clock.sendFrame(200_000_000L) // 200ms
            runCurrent()

            assertTrue(anim.value > 0f && anim.value < 1000f)

            anim.value = 500f

            assertEquals(500f, anim.value)
            assertEquals(0f, anim.velocity)
            assertTrue(animationJob.isCancelled)

            clock.sendFrame(500_000_000L)
            runCurrent()
            assertEquals(500f, anim.value)
        }
    }

    @Test
    fun givenInterruptedValue_whenNewAnimationStarted_thenAnimatesFromInterruptedValue() = runTest {
        val clock = BroadcastFrameClock()
        withContext(clock) {
            val anim = AnimatedFloat(0f)

            val firstJob = launch {
                anim.animateTo(
                    target = 100f,
                    spec = tween(durationMillis = 500)
                )
            }

            runCurrent()
            clock.sendFrame(0L)
            runCurrent()
            clock.sendFrame(100_000_000L)
            runCurrent()

            anim.value = 250f
            assertEquals(250f, anim.value)

            val secondJob = launch {
                anim.animateTo(
                    target = 300f,
                    spec = tween(durationMillis = 500)
                )
            }

            runCurrent()
            clock.sendFrame(200_000_000L)
            runCurrent()
            clock.sendFrame(350_000_000L)
            runCurrent()

            assertTrue(anim.value in 250f..300f)

            clock.sendFrame(1_000_000_000L)
            runCurrent()
            assertEquals(300f, anim.value)
            assertTrue(firstJob.isCancelled)
        }
    }

    @Test
    fun givenMultipleTypes_whenRepeatedlyInterruptedUnderStress_thenAllMaintainStateIntegrity() = runTest {
        val clock = BroadcastFrameClock()
        withContext(clock) {
            val floatAnim = AnimatedFloat(0f)
            val intAnim = AnimatedInt(0)
            val offsetAnim = AnimatedOffset(Offset.Zero)
            val intOffsetAnim = AnimatedIntOffset(IntOffset.Zero)

            for (cycle in 1..50) {
                val job = launch {
                    launch { floatAnim.animateTo(1000f, tween(500)) }
                    launch { intAnim.animateTo(1000, tween(500)) }
                    launch { offsetAnim.animateTo(Offset(1000f, 1000f), tween(500)) }
                    launch { intOffsetAnim.animateTo(IntOffset(1000, 1000), tween(500)) }
                }

                runCurrent()
                clock.sendFrame((cycle * 100_000_000L))
                runCurrent()

                val snapValue = cycle * 10
                floatAnim.value = snapValue.toFloat()
                intAnim.value = snapValue
                offsetAnim.value = Offset(snapValue.toFloat(), snapValue.toFloat())
                intOffsetAnim.value = IntOffset(snapValue, snapValue)

                assertEquals(snapValue.toFloat(), floatAnim.value)
                assertEquals(snapValue, intAnim.value)
                assertEquals(Offset(snapValue.toFloat(), snapValue.toFloat()), offsetAnim.value)
                assertEquals(IntOffset(snapValue, snapValue), intOffsetAnim.value)

                clock.sendFrame((cycle * 100_000_000L + 500_000_000L))
                runCurrent()

                assertEquals(snapValue.toFloat(), floatAnim.value)
                assertEquals(snapValue, intAnim.value)
                assertEquals(Offset(snapValue.toFloat(), snapValue.toFloat()), offsetAnim.value)
                assertEquals(IntOffset(snapValue, snapValue), intOffsetAnim.value)
            }
        }
    }
}
