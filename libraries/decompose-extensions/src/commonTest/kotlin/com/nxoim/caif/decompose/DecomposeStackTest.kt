package com.nxoim.caif.decompose

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds

class DecomposeStackTest {

    private sealed interface ScreenConfig {
        data object Feed : ScreenConfig
        data class PostDetail(val postId: String) : ScreenConfig
        data object SettingsDialog : ScreenConfig
        data object Profile : ScreenConfig
    }

    private sealed interface ScreenChild {
        data object Feed : ScreenChild
        data class PostDetail(val postId: String) : ScreenChild
        data object SettingsDialog : ScreenChild
        data object Profile : ScreenChild
    }

    @Test
    fun givenDecomposeDsl_whenExhaustiveMatchingUsed_thenCorrectPlatformCapabilitiesAreProvisioned() {
        val animationFactory = decomposeAnimations<ScreenConfig, ScreenChild> { child ->
            when (child) {
                ScreenChild.Feed -> adaptiveStackAnimation()
                is ScreenChild.PostDetail -> CupertinoStackAnimation()
                ScreenChild.SettingsDialog -> MaterialStackAnimation()
                ScreenChild.Profile -> selectableStackAnimation(
                    swipe = { CupertinoStackAnimation() },
                    predictiveBack = { MaterialStackAnimation() },
                )
            }
        }

        val feedAnim = animationFactory.create(ScreenChild.Feed, ScreenConfig.Feed)
        assertNotNull(feedAnim)
        assertNotNull(feedAnim.getAndSelectCapability(SwipeCapability::class), "adaptive must support interactive swipe")
        assertNotNull(feedAnim.getAndSelectCapability(PredictiveBackCapability::class), "adaptive must support predictive back")

        val postDetailAnim = animationFactory.create(ScreenChild.PostDetail("42"), ScreenConfig.PostDetail("42"))
        assertNotNull(postDetailAnim)
        assertNotNull(postDetailAnim.getAndSelectCapability(SwipeCapability::class), "cupertino must support swipe input")

        val dialogAnim = animationFactory.create(ScreenChild.SettingsDialog, ScreenConfig.SettingsDialog)
        assertNotNull(dialogAnim)
        assertNotNull(dialogAnim.getAndSelectCapability(PredictiveBackCapability::class), "material must support predictive back input")

        val profileAnim = animationFactory.create(ScreenChild.Profile, ScreenConfig.Profile)
        assertNotNull(profileAnim)
        assertNotNull(profileAnim.getAndSelectCapability(SwipeCapability::class))
        assertNotNull(profileAnim.getAndSelectCapability(PredictiveBackCapability::class))
    }

    @Test
    fun givenCustomDefaultAnimation_whenChildMatchesFallback_thenDefaultAnimationIsApplied() {
        val fallbackFactory = decomposeAnimations<ScreenConfig, ScreenChild>(
            default = { CupertinoStackAnimation(slideSpringDuration = 400.milliseconds) },
            selector = { child ->
                when (child) {
                    ScreenChild.Feed -> MaterialStackAnimation()
                    else -> null
                }
            },
        )

        val feedAnim = fallbackFactory.create(ScreenChild.Feed, ScreenConfig.Feed)
        assertNotNull(feedAnim.getAndSelectCapability(PredictiveBackCapability::class))

        val postAnim = fallbackFactory.create(ScreenChild.PostDetail("1"), ScreenConfig.PostDetail("1"))
        assertNotNull(postAnim.getAndSelectCapability(SwipeCapability::class))
    }

    @Test
    fun givenSelectableAnimation_whenCapabilitySelected_thenActiveDispatcherSwitchesSeamlessly() {
        val selectableAnim = selectableStackAnimation(
            swipe = { CupertinoStackAnimation() },
            predictiveBack = { MaterialStackAnimation() },
        )

        val swipeCapability = selectableAnim.getAndSelectCapability(SwipeCapability::class)
        assertNotNull(swipeCapability)

        val predictiveCapability = selectableAnim.getAndSelectCapability(PredictiveBackCapability::class)
        assertNotNull(predictiveCapability)
    }
}
