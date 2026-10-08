package com.nxoim.caif.decompose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.arkivanov.decompose.Child.Created
import com.nxoim.caif.prefabs.stack.ItemAnimationRegistry
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import com.nxoim.caif.prefabs.stack.StackOrchestrator
import com.nxoim.caif.prefabs.stack.defaultStackContextResolver
import com.nxoim.caif.springs.springA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class DecomposeStackTest {

    @Test
    fun givenDeclarationBlock_whenUsedAsTrailingLambda_thenDefaultIsDeclaredWithoutAmbiguity() {
        val factory = decomposeStackAnimations<ScreenConfig, ScreenChild> { _, _ ->
            defaultAnimation { CupertinoStackAnimation() }
        }
        val entry = Created<ScreenConfig, ScreenChild>(ScreenConfig.Feed, ScreenChild.Feed)
        assertNotNull(factory.create(entry, entry.configuration).getAndSelectCapability(SwipeCapability::class))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun givenDestinationCallbacks_whenInputChanges_thenTopmostConfigurationAndChildAreUsed() = runTest {
        val feed = Created<ScreenConfig, ScreenChild>(ScreenConfig.Feed, ScreenChild.Feed)
        val detail = Created<ScreenConfig, ScreenChild>(ScreenConfig.PostDetail("42"), ScreenChild.PostDetail("42"))
        val selected = mutableListOf<Pair<ScreenConfig, ScreenChild>>()
        val predictiveInstances = mutableListOf<MaterialStackAnimation>()
        val factory = decomposeStackAnimations<ScreenConfig, ScreenChild>(
            swipe = { configuration, child ->
                selected += configuration to child
                CupertinoStackAnimation()
            },
            predictiveBack = { configuration, child ->
                selected += configuration to child
                MaterialStackAnimation().also { predictiveInstances += it }
            },
        )
        assertNotNull(factory.create(feed, feed.configuration).getAndSelectCapability(SwipeCapability::class))
        assertEquals(listOf(feed.configuration to feed.instance), selected)
        val stack = mutableStateOf(listOf(feed, detail))
        assertSame(detail, factory.sourceItem(null, stack.value))
        val environment = StackAnimationEnvironment(Size(100f, 100f), LayoutDirection.Ltr, Density(1f))
        val orchestrator = StackOrchestrator(
            scope = CoroutineScope(backgroundScope.coroutineContext + BroadcastFrameClock()),
            stack = stack,
            registry = ItemAnimationRegistry(factory),
            resolver = defaultStackContextResolver(
                contextFactory = stackAnimationContextFactory { environment },
                keyFor = { entry: Created<ScreenConfig, ScreenChild> -> entry.configuration },
            ),
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy.byStackIndex<ScreenConfig>(),
        )
        runCurrent()
        selected.clear()
        val capabilities = orchestrator.startCycle(PredictiveBackCapability::class)
        assertEquals(2, capabilities.size)
        assertEquals(listOf(detail.configuration to detail.instance, detail.configuration to detail.instance), selected)
        assertEquals(2, predictiveInstances.size)
        capabilities.values.forEach { capability ->
            assertNotNull(capability)
            assertTrue(predictiveInstances.any { it === capability })
        }
    }

    @Test
    fun givenDecomposeDsl_whenExhaustiveMatchingUsed_thenCorrectPlatformCapabilitiesAreProvisioned() {
        val animationFactory = decomposeStackAnimations<ScreenConfig, ScreenChild> { _, child ->
            when (child) {
                ScreenChild.Feed, ScreenChild.Profile -> {
                    val swipe = onInput<SwipeCapability> { CupertinoStackAnimation() }
                    onInput<PredictiveBackCapability> { MaterialStackAnimation() }
                    defaultAnimation(swipe)
                }
                is ScreenChild.PostDetail -> defaultAnimation { CupertinoStackAnimation() }
                ScreenChild.SettingsDialog -> defaultAnimation { MaterialStackAnimation() }
            }
        }

        val feedAnim = animationFactory.create(Created(ScreenConfig.Feed, ScreenChild.Feed), ScreenConfig.Feed)
        assertNotNull(feedAnim)
        assertNotNull(feedAnim.getAndSelectCapability(SwipeCapability::class), "the default must support interactive swipe")
        assertNull(feedAnim.getAndSelectCapability(PredictiveBackCapability::class), "input selection happens at cycle start")

        val postDetailAnim = animationFactory.create(
            Created(ScreenConfig.PostDetail("42"), ScreenChild.PostDetail("42")), ScreenConfig.PostDetail("42"),
        )
        assertNotNull(postDetailAnim)
        assertNotNull(postDetailAnim.getAndSelectCapability(SwipeCapability::class), "cupertino must support swipe input")

        val dialogAnim = animationFactory.create(
            Created(ScreenConfig.SettingsDialog, ScreenChild.SettingsDialog), ScreenConfig.SettingsDialog,
        )
        assertNotNull(dialogAnim)
        assertNotNull(dialogAnim.getAndSelectCapability(PredictiveBackCapability::class), "material must support predictive back input")

        val profileAnim = animationFactory.create(Created(ScreenConfig.Profile, ScreenChild.Profile), ScreenConfig.Profile)
        assertNotNull(profileAnim)
        assertNotNull(profileAnim.getAndSelectCapability(SwipeCapability::class))
        assertNull(profileAnim.getAndSelectCapability(PredictiveBackCapability::class))
    }

    @Test
    fun givenCustomDefaultAnimation_whenChildMatchesFallback_thenDefaultAnimationIsApplied() {
        val fallbackFactory = decomposeStackAnimations<ScreenConfig, ScreenChild> { _, child ->
            defaultAnimation {
                when (child) {
                    ScreenChild.Feed -> MaterialStackAnimation()
                    else -> CupertinoStackAnimation(
                        slideSpec = { springA(duration = 400.milliseconds) },
                        overlayAlphaSpec = { springA(duration = 400.milliseconds) },
                    )
                }
            }
        }

        val feedAnim = fallbackFactory.create(Created(ScreenConfig.Feed, ScreenChild.Feed), ScreenConfig.Feed)
        assertNotNull(feedAnim.getAndSelectCapability(PredictiveBackCapability::class))

        val postAnim = fallbackFactory.create(
            Created(ScreenConfig.PostDetail("1"), ScreenChild.PostDetail("1")), ScreenConfig.PostDetail("1"),
        )
        assertNotNull(postAnim.getAndSelectCapability(SwipeCapability::class))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun givenInputDeclarations_whenOrdinaryCycleStarts_thenSwipeDefaultIsRestored() = runTest {
        val entry = Created<ScreenConfig, ScreenChild>(ScreenConfig.Feed, ScreenChild.Feed)
        val stack = mutableStateOf(listOf(entry))
        val factory = decomposeStackAnimations<ScreenConfig, ScreenChild>()
        val environment = StackAnimationEnvironment(Size(100f, 100f), LayoutDirection.Ltr, Density(1f))
        val orchestrator = StackOrchestrator(
            scope = CoroutineScope(backgroundScope.coroutineContext + BroadcastFrameClock()),
            stack = stack,
            registry = ItemAnimationRegistry(factory),
            resolver = defaultStackContextResolver(
                contextFactory = stackAnimationContextFactory { environment },
                keyFor = { item: Created<ScreenConfig, ScreenChild> -> item.configuration },
            ),
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy.byStackIndex<ScreenConfig>(),
        )
        runCurrent()
        assertNotNull(orchestrator.startCycle(PredictiveBackCapability::class).getValue(ScreenConfig.Feed))
        val animation = orchestrator.itemsToRender.single().second
        assertNull(animation.getAndSelectCapability(SwipeCapability::class))

        orchestrator.startCycle(stack.value)
        assertNotNull(animation.getAndSelectCapability(SwipeCapability::class))
        assertNull(animation.getAndSelectCapability(PredictiveBackCapability::class))
    }

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
}
