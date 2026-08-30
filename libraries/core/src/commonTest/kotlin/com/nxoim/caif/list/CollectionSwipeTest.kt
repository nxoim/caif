package com.nxoim.caif.list

import androidx.compose.ui.Modifier
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.prefabs.list.CollectionItemPosition
import com.nxoim.caif.prefabs.list.CollectionSwipeAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionSwipeTest {

    @Test
    fun givenActiveList_whenGestureStarted_thenNeighborDistancesTrackRelativeOffsetsWithoutResetting() = runTest {
        val visibleItems = mutableListOf("a", "b", "c", "d")
        val animations = mutableMapOf<String, SpecCapabilityCollectionAnimation>()
        val animator = createCollectionAnimator(visibleItems, animations, backgroundScope)

        animator.onStart("b")
        runCurrent()
        advanceUntilIdle()

        animator.activeGestureCapabilities(SpecCollectionCapability::class)
            .forEach { (_, value) -> value.first?.recordedDistances?.add(value.second) }

        assertEquals(listOf(1), animations.getValue("a").capability.recordedDistances)
        assertEquals(listOf(0), animations.getValue("b").capability.recordedDistances)
        assertEquals(listOf(1), animations.getValue("c").capability.recordedDistances)
        assertEquals(listOf(2), animations.getValue("d").capability.recordedDistances)

        animations.values.forEach { it.settledTargets.clear() }
        animator.onStart("c")
        runCurrent()
        advanceUntilIdle()
        assertTrue(animations.values.all { it.settledTargets.isEmpty() })

        animator.releaseFromGesture("d")
        runCurrent()
        advanceUntilIdle()

        assertFalse("d" in animator.activeGestureCapabilities(SpecCollectionCapability::class))
        assertEquals(listOf(CollectionItemPosition(3, null)), animations.getValue("d").settledTargets)
    }

    @Test
    fun givenSwipeMenu_whenGestureEndsWithPreservePosition_thenDisplacedOffsetIsRetained() = runTest {
        val visibleItems = mutableListOf("menuItem")
        val animation = SpecCollectionRecordingAnimation()
        val animator = CollectionSwipeAnimator(
            settleableFactory = { animation },
            contextFor = { it },
            scope = backgroundScope,
            visibleKeys = { visibleItems },
        )

        animator.onStart("menuItem")
        runCurrent()
        advanceUntilIdle()
        animation.settledTargets.clear()

        animator.onEnd(preservePositionForKey = "menuItem")
        runCurrent()
        advanceUntilIdle()

        assertTrue(animation.settledTargets.isEmpty())
        assertTrue(animator.activeGestureCapabilities(String::class).isEmpty())
    }

    @Test
    fun givenInvalidVisibleKeysOrMissingOrigin_whenGestureTriggered_thenFailsFast() = runTest {
        val duplicateKeysAnimator = CollectionSwipeAnimator(
            settleableFactory = { SpecCollectionRecordingAnimation() },
            contextFor = { it },
            scope = backgroundScope,
            visibleKeys = { listOf("item", "item") },
        )
        assertFailsWith<IllegalArgumentException> { duplicateKeysAnimator.onStart("item") }

        val missingOriginAnimator = CollectionSwipeAnimator(
            settleableFactory = { SpecCollectionRecordingAnimation() },
            contextFor = { it },
            scope = backgroundScope,
            visibleKeys = { listOf("itemA") },
        )
        assertFailsWith<IllegalArgumentException> { missingOriginAnimator.onStart("missingItem") }
    }
}

private class SpecCollectionCapability {
    val recordedDistances = mutableListOf<Int>()
}

private class SpecCapabilityCollectionAnimation : ItemAnimation<CollectionItemPosition> {
    override val modifier = Modifier
    val capability = SpecCollectionCapability()
    val settledTargets = mutableListOf<CollectionItemPosition>()

    override fun reset(context: CollectionItemPosition) = Unit

    override suspend fun animateTo(target: CollectionItemPosition) {
        settledTargets += target
    }

    override fun willBeVisible(context: CollectionItemPosition) = true

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? =
        capability.takeIf { kClass == SpecCollectionCapability::class } as T?
}

private class SpecCollectionRecordingAnimation : ItemAnimation<CollectionItemPosition> {
    override val modifier = Modifier
    val settledTargets = mutableListOf<CollectionItemPosition>()

    override fun reset(context: CollectionItemPosition) = Unit
    override suspend fun animateTo(target: CollectionItemPosition) {
        settledTargets += target
    }
    override fun willBeVisible(context: CollectionItemPosition) = true
    override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? = null
}

private fun createCollectionAnimator(
    visible: MutableList<String>,
    animations: MutableMap<String, SpecCapabilityCollectionAnimation>,
    scope: CoroutineScope,
): CollectionSwipeAnimator<String, CollectionItemPosition> =
    CollectionSwipeAnimator(
        settleableFactory = { item ->
            SpecCapabilityCollectionAnimation().also { animations[item] = it }
        },
        contextFor = { position -> position },
        scope = scope,
        visibleKeys = { visible },
    )
