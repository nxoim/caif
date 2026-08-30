package com.nxoim.caif.core

import androidx.compose.ui.Modifier
import com.nxoim.caif.utils.typeMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeMapCapabilityTest {

    private interface SwipeCap {
        fun onSwipe(progress: Float)
    }

    private interface PredictiveBackCap {
        fun onBackProgress(progress: Float)
    }

    private class SwipeImpl : SwipeCap {
        var progress = 0f
        override fun onSwipe(progress: Float) {
            this.progress = progress
        }
    }

    private class PredictiveBackImpl : PredictiveBackCap {
        var progress = 0f
        override fun onBackProgress(progress: Float) {
            this.progress = progress
        }
    }

    @Test
    fun givenTypeMap_whenStoringMultipleTypes_thenAllowsTypeSafeRetrieval() {
        val map = typeMap()
        val swipe = SwipeImpl()
        val back = PredictiveBackImpl()

        map.put(SwipeCap::class, swipe)
        map.put(PredictiveBackCap::class, back)

        assertTrue(SwipeCap::class in map)
        assertTrue(PredictiveBackCap::class in map)

        val retrievedSwipe = map.get(SwipeCap::class)
        assertNotNull(retrievedSwipe)
        retrievedSwipe.onSwipe(0.5f)
        assertEquals(0.5f, swipe.progress)

        val retrievedBack = map.get(PredictiveBackCap::class)
        assertNotNull(retrievedBack)
        retrievedBack.onBackProgress(0.8f)
        assertEquals(0.8f, back.progress)
    }

    private class CombinedAnimation : BaseItemAnimation<String>(), SwipeCap, PredictiveBackCap {
        override val modifier: Modifier = Modifier
        var swipeProgress = 0f
        var backProgress = 0f

        override fun onSwipe(progress: Float) {
            swipeProgress = progress
        }

        override fun onBackProgress(progress: Float) {
            backProgress = progress
        }
    }

    private class ChildSwipeAnimation : BaseItemAnimation<String>(), SwipeCap {
        override val modifier: Modifier = Modifier
        override fun onSwipe(progress: Float) {}
    }

    private class ParentCompositeAnimation : BaseItemAnimation<String>() {
        override val modifier: Modifier = Modifier
        val child = registerChild(ChildSwipeAnimation())
    }

    @Test
    fun givenBaseItemAnimation_whenCapabilitiesImplementedDirectly_thenAllAreAccessible() {
        val animation = CombinedAnimation()

        val swipe = animation.getAndSelectCapability<SwipeCap>()
        assertNotNull(swipe)
        swipe.onSwipe(0.7f)
        assertEquals(0.7f, animation.swipeProgress)

        val back = animation.getAndSelectCapability<PredictiveBackCap>()
        assertNotNull(back)
        back.onBackProgress(0.9f)
        assertEquals(0.9f, animation.backProgress)

        assertNull(animation.getAndSelectCapability<String>())
    }

    @Test
    fun givenBaseItemAnimation_whenChildHasCapability_thenForwardsToChild() {
        val animation = ParentCompositeAnimation()

        val swipe = animation.getAndSelectCapability<SwipeCap>()
        assertNotNull(swipe)
        assertNull(animation.getAndSelectCapability<PredictiveBackCap>())
    }
}
