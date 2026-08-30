package com.nxoim.caif.core

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nxoim.caif.core.base.mutableColorStateOf
import com.nxoim.caif.core.base.mutableDpOffsetStateOf
import com.nxoim.caif.core.base.mutableDpSizeStateOf
import com.nxoim.caif.core.base.mutableDpStateOf
import com.nxoim.caif.core.base.mutableIntOffsetStateOf
import com.nxoim.caif.core.base.mutableIntSizeStateOf
import com.nxoim.caif.core.base.mutableOffsetStateOf
import com.nxoim.caif.core.base.mutableSizeStateOf
import com.nxoim.caif.core.base.mutableVelocityStateOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OptimizedStatesTest {

    @Test
    fun givenPackedOffsetState_whenMutated_thenPreservesSubpixelPrecisionAndNegativeValues() {
        val state = mutableOffsetStateOf(Offset(0f, 0f))

        val testCases = listOf(
            Offset(123.456f, -789.012f),
            Offset(-0.001f, 0.001f),
            Offset(Float.MAX_VALUE / 2, -Float.MAX_VALUE / 2),
            Offset.Zero
        )

        for (expected in testCases) {
            state.value = expected
            assertEquals(expected.x, state.value.x, 0.0001f)
            assertEquals(expected.y, state.value.y, 0.0001f)
        }
    }

    @Test
    fun givenPackedIntOffsetState_whenMutated_thenPreservesIntegerBoundaries() {
        val state = mutableIntOffsetStateOf(IntOffset(0, 0))

        val testCases = listOf(
            IntOffset(Int.MAX_VALUE / 2, Int.MIN_VALUE / 2),
            IntOffset(-42, 42),
            IntOffset(1920, 1080),
            IntOffset.Zero
        )

        for (expected in testCases) {
            state.value = expected
            assertEquals(expected, state.value)
        }
    }

    @Test
    fun givenPackedSizeAndIntSizeStates_whenMutated_thenPreservesDimensions() {
        val sizeState = mutableSizeStateOf(Size.Zero)
        val intSizeState = mutableIntSizeStateOf(IntSize.Zero)

        sizeState.value = Size(1080.5f, 1920.75f)
        assertEquals(1080.5f, sizeState.value.width, 0.0001f)
        assertEquals(1920.75f, sizeState.value.height, 0.0001f)

        intSizeState.value = IntSize(3840, 2160)
        assertEquals(IntSize(3840, 2160), intSizeState.value)
    }

    @Test
    fun givenPackedColorState_whenMutated_thenPreservesArgbChannels() {
        val state = mutableColorStateOf(Color.Unspecified)

        val testColors = listOf(
            Color.Red,
            Color.Green,
            Color.Blue,
            Color(0x80FF5722),
            Color(red = 0.25f, green = 0.5f, blue = 0.75f, alpha = 0.5f)
        )

        for (expected in testColors) {
            state.value = expected
            assertEquals(expected.value, state.value.value)
        }
    }

    @Test
    fun givenPackedVelocityState_whenMutated_thenPreservesVectorMagnitudes() {
        val state = mutableVelocityStateOf(Velocity.Zero)

        state.value = Velocity(2500.5f, -1800.25f)
        assertEquals(2500.5f, state.value.x, 0.0001f)
        assertEquals(-1800.25f, state.value.y, 0.0001f)
    }

    @Test
    fun givenPackedDpStates_whenMutated_thenPreservesUnits() {
        val dpState = mutableDpStateOf(0.dp)
        dpState.value = 16.5.dp
        assertEquals(16.5.dp, dpState.value)

        val dpOffsetState = mutableDpOffsetStateOf(DpOffset.Zero)
        dpOffsetState.value = DpOffset(8.dp, (-24).dp)
        assertEquals(DpOffset(8.dp, (-24).dp), dpOffsetState.value)

        val dpSizeState = mutableDpSizeStateOf(DpSize.Zero)
        dpSizeState.value = DpSize(200.dp, 400.dp)
        assertEquals(DpSize(200.dp, 400.dp), dpSizeState.value)
    }

    @Test
    fun givenOptimizedState_whenObservedViaSnapshotFlow_thenTriggersReactivityAccurately() = runTest {
        val offsetState = mutableOffsetStateOf(Offset.Zero)
        val observedValues = mutableListOf<Offset>()

        val job = backgroundScope.launch {
            snapshotFlow { offsetState.value }
                .take(3)
                .toList(observedValues)
        }
        runCurrent()

        Snapshot.withMutableSnapshot {
            offsetState.value = Offset(10f, 20f)
        }
        runCurrent()

        Snapshot.withMutableSnapshot {
            offsetState.value = Offset(30f, 40f)
        }
        runCurrent()

        assertEquals(
            listOf(Offset.Zero, Offset(10f, 20f), Offset(30f, 40f)),
            observedValues
        )
        job.cancel()
    }
}
