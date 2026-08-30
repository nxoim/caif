package com.nxoim.caif.stack

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.prefabs.stack.AffectedItemsPolicy
import com.nxoim.caif.prefabs.stack.AppearanceIntention
import com.nxoim.caif.prefabs.stack.ContextFactory
import com.nxoim.caif.prefabs.stack.ContextResolver
import com.nxoim.caif.prefabs.stack.ItemAnimationRegistry
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import com.nxoim.caif.prefabs.stack.StackCreationContext
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.StackOrchestrator
import com.nxoim.caif.prefabs.stack.defaultStackContextResolver
import com.nxoim.caif.prefabs.stack.indexOf
import com.nxoim.caif.prefabs.stack.previousIndexOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StackAnimationTest {

    @Test
    fun givenInitialStack_whenPushed_thenNewItemPreEntersAndSettlesWhileExistingShiftsDepth() = runTest {
        val stack = mutableStateOf(listOf("home"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
        )
        runCurrent()
        advanceUntilIdle()

        updateState(stack, listOf("home", "detail"))
        runCurrent()
        advanceUntilIdle()

        val homeAnim = animations.getValue("home")
        val detailAnim = animations.getValue("detail")

        assertEquals(StackItemPosition.Inside(index = 1, previousIndex = null), detailAnim.targets.last())
        assertEquals(StackItemPosition.Inside(index = 0, previousIndex = 0), homeAnim.targets.last())
        assertEquals(setOf("home", "detail"), orchestrator.renderedKeys())
    }

    @Test
    fun givenStackWithItems_whenPopped_thenPoppedItemAnimatesToRemovedAndUnmountsWhenComplete() = runTest {
        val stack = mutableStateOf(listOf("home", "detail"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()
        val detailExitCompleted = CompletableDeferred<Unit>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
            removalDeferred = mapOf("detail" to detailExitCompleted),
        )
        runCurrent()
        advanceUntilIdle()

        updateState(stack, listOf("home"))
        runCurrent()
        advanceUntilIdle()

        val detailAnim = animations.getValue("detail")
        assertEquals(StackItemPosition.Removed, detailAnim.targets.last())
        assertTrue("detail" in orchestrator.renderedKeys())

        detailExitCompleted.complete(Unit)
        runCurrent()
        advanceUntilIdle()

        assertEquals(setOf("home"), orchestrator.renderedKeys())
    }

    @Test
    fun givenStack_whenScreenReplaced_thenOldItemAnimatesOutAndNewItemAnimatesInConcurrently() = runTest {
        val stack = mutableStateOf(listOf("root", "screenA"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()
        val screenAExitCompleted = CompletableDeferred<Unit>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
            removalDeferred = mapOf("screenA" to screenAExitCompleted),
        )
        runCurrent()
        advanceUntilIdle()

        updateState(stack, listOf("root", "screenB"))
        runCurrent()
        advanceUntilIdle()

        assertEquals(setOf("root", "screenA", "screenB"), orchestrator.renderedKeys())
        assertEquals(StackItemPosition.Removed, animations.getValue("screenA").targets.last())
        assertEquals(StackItemPosition.Inside(index = 1, previousIndex = null), animations.getValue("screenB").targets.last())

        screenAExitCompleted.complete(Unit)
        runCurrent()
        advanceUntilIdle()

        assertEquals(setOf("root", "screenB"), orchestrator.renderedKeys())
    }

    @Test
    fun givenDeepStack_whenPoppedToRoot_thenAllPoppedItemsAnimateToRemovedSimultaneously() = runTest {
        val stack = mutableStateOf(listOf("root", "a", "b", "c"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()
        val exits = mapOf(
            "a" to CompletableDeferred<Unit>(),
            "b" to CompletableDeferred(),
            "c" to CompletableDeferred(),
        )

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
            removalDeferred = exits,
        )
        runCurrent()
        advanceUntilIdle()

        updateState(stack, listOf("root"))
        runCurrent()
        advanceUntilIdle()

        assertEquals(setOf("root", "a", "b", "c"), orchestrator.renderedKeys())
        listOf("a", "b", "c").forEach { key ->
            assertEquals(StackItemPosition.Removed, animations.getValue(key).targets.last())
        }

        exits.getValue("c").complete(Unit)
        runCurrent()
        advanceUntilIdle()
        assertEquals(setOf("root", "a", "b"), orchestrator.renderedKeys())

        exits.getValue("b").complete(Unit)
        exits.getValue("a").complete(Unit)
        runCurrent()
        advanceUntilIdle()
        assertEquals(setOf("root"), orchestrator.renderedKeys())
    }

    @Test
    fun givenStack_whenReordered_thenRenderOrderStrategyPreservesDepthAndOrder() = runTest {
        val stack = mutableStateOf(listOf("a", "b", "c", "d"))
        val exitDeferred = CompletableDeferred<Unit>()

        val orchestrator = StackOrchestrator(
            scope = backgroundScope,
            stack = stack,
            registry = ItemAnimationRegistry { _, key ->
                SpecRecordingAnimation(exitDeferred.takeIf { key == "b" })
            },
            resolver = specStackResolver,
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy.byStackIndex(),
        )
        runCurrent()
        advanceUntilIdle()

        assertEquals(listOf("d", "c", "b", "a"), orchestrator.itemsToRender.map { it.first.first })

        updateState(stack, listOf("d", "c", "a"))
        runCurrent()
        advanceUntilIdle()

        assertEquals(listOf("a", "c", "b", "d"), orchestrator.itemsToRender.map { it.first.first })

        exitDeferred.complete(Unit)
        runCurrent()
        advanceUntilIdle()

        assertEquals(listOf("a", "c", "d"), orchestrator.itemsToRender.map { it.first.first })
    }

    @Test
    fun givenInFlightRemoval_whenItemReAddedBeforeExitFinishes_thenRemovalIsCancelledAndItemRestored() = runTest {
        val stack = mutableStateOf(listOf("home", "profile"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()
        val profileExitDeferred = CompletableDeferred<Unit>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
            removalDeferred = mapOf("profile" to profileExitDeferred),
        )
        runCurrent()
        advanceUntilIdle()

        updateState(stack, listOf("home"))
        runCurrent()
        advanceUntilIdle()
        assertEquals(StackItemPosition.Removed, animations.getValue("profile").targets.last())

        updateState(stack, listOf("home", "profile"))
        runCurrent()
        advanceUntilIdle()

        assertEquals(StackItemPosition.Inside(index = 1, previousIndex = null), animations.getValue("profile").targets.last())
        assertEquals(setOf("home", "profile"), orchestrator.renderedKeys())

        profileExitDeferred.complete(Unit)
        runCurrent()
        advanceUntilIdle()

        assertEquals(setOf("home", "profile"), orchestrator.renderedKeys())
    }

    @Test
    fun givenThousandRandomMutations_whenAppliedRapidly_thenOrchestratorResolvesExactFinalStateWithoutLeaks() = runTest {
        val stack = mutableStateOf(listOf("root"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
            maxAffected = 10,
        )
        runCurrent()
        advanceUntilIdle()

        val random = Random(12345)
        val availableKeys = (1..60).map { "page_$it" }
        var currentStack = listOf("root")

        for (i in 1..1000) {
            val action = random.nextInt(4)
            currentStack = when (action) {
                0 -> {
                    val candidate = availableKeys.filter { it !in currentStack }.randomOrNull(random)
                    if (candidate != null) currentStack + candidate else currentStack
                }
                1 -> {
                    if (currentStack.size > 1) currentStack.dropLast(1) else currentStack
                }
                2 -> {
                    if (currentStack.size > 2) currentStack.shuffled(random) else currentStack
                }
                else -> {
                    val candidate = availableKeys.filter { it !in currentStack }.randomOrNull(random)
                    if (candidate != null && currentStack.isNotEmpty()) {
                        currentStack.dropLast(1) + candidate
                    } else currentStack
                }
            }

            Snapshot.withMutableSnapshot { stack.value = currentStack }
            if (i % 7 == 0) runCurrent()
        }

        runCurrent()
        advanceUntilIdle()

        val renderedKeys = orchestrator.renderedKeys()
        val expectedVisibleKeys = currentStack.takeLast(10)
        for (item in expectedVisibleKeys) {
            assertTrue(item in renderedKeys, "Item $item from visible stack tail must be rendered")
        }
    }

    @Test
    fun givenEmptyStackTransitions_whenEmptiedAndRepopulated_thenOrchestratorRecoversCleanly() = runTest {
        val stack = mutableStateOf(listOf("a", "b"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
        )
        runCurrent()
        advanceUntilIdle()
        assertEquals(2, orchestrator.itemsToRender.size)

        updateState(stack, emptyList())
        runCurrent()
        advanceUntilIdle()
        assertEquals(0, orchestrator.itemsToRender.size)

        updateState(stack, listOf("x", "y", "z"))
        runCurrent()
        advanceUntilIdle()
        assertEquals(setOf("x", "y", "z"), orchestrator.renderedKeys())
    }

    @Test
    fun givenLargeStackScale_whenUpdated_thenAffectedItemsPolicyBoundsExecutionWork() = runTest {
        val largeSize = 2_000
        val items = (0 until largeSize).map { "item_$it" }
        val stack = mutableStateOf(emptyList<String>())
        val animations = mutableMapOf<String, SpecRecordingAnimation>()

        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
            maxAffected = 10,
            policy = AffectedItemsPolicy.fromTop(reversed = true),
        )

        updateState(stack, items)
        runCurrent()
        advanceUntilIdle()

        assertEquals(largeSize, stack.value.size)
        assertTrue(animations.size <= 20, "Should only create animation instances for items within the affected limit")
    }

    @Test
    fun givenExternalAnimationsRegistered_whenItemPopped_thenUnmountingWaitsForExternalSignals() = runTest {
        val stack = mutableStateOf(listOf("a", "b"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()
        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
        )
        runCurrent()
        advanceUntilIdle()

        val isSharedElementMorphing = mutableStateOf(true)
        val isCustomParticleEffectRunning = mutableStateOf(true)

        orchestrator.registerExternalAnimation("b") { isSharedElementMorphing.value }
        val particleReg = orchestrator.registerExternalAnimation("b") { isCustomParticleEffectRunning.value }

        updateState(stack, listOf("a"))
        runCurrent()
        advanceUntilIdle()

        assertTrue("b" in orchestrator.renderedKeys())

        updateState(isSharedElementMorphing, false)
        runCurrent()
        advanceUntilIdle()
        assertTrue("b" in orchestrator.renderedKeys())

        Snapshot.withMutableSnapshot { particleReg.unregister() }
        runCurrent()
        advanceUntilIdle()

        assertFalse("b" in orchestrator.renderedKeys())
    }

    @Test
    fun givenFailingRemovalAnimation_whenExceptionThrown_thenFaultIsIsolatedAndItemEvicted() = runTest {
        val caughtExceptions = mutableListOf<Throwable>()
        val isolatedScope = CoroutineScope(
            StandardTestDispatcher(testScheduler) +
                    SupervisorJob() +
                    CoroutineExceptionHandler { _, throwable -> caughtExceptions += throwable }
        )
        val stack = mutableStateOf(listOf("a", "faulty"))

        val orchestrator = StackOrchestrator(
            scope = isolatedScope,
            stack = stack,
            registry = ItemAnimationRegistry { _, key ->
                if (key == "faulty") {
                    object : ItemAnimation<StackItemPosition> {
                        override val modifier = Modifier
                        override fun reset(context: StackItemPosition) = Unit
                        override suspend fun animateTo(target: StackItemPosition) {
                            if (target is StackItemPosition.Removed) error("Simulated animation crash")
                        }
                        override fun willBeVisible(context: StackItemPosition) = true
                        override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? = null
                    }
                } else {
                    SpecRecordingAnimation()
                }
            },
            resolver = specStackResolver,
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy.insertionOrder(),
        )
        runCurrent()
        advanceUntilIdle()

        updateState(stack, listOf("a"))
        runCurrent()
        advanceUntilIdle()

        assertFalse("faulty" in orchestrator.renderedKeys())
        assertEquals(1, caughtExceptions.size)
        isolatedScope.coroutineContext[Job]?.cancel()
    }

    @Test
    fun givenDisposedOrchestrator_whenDisposed_thenAllJobsCancelledAndCachesCleared() = runTest {
        val stack = mutableStateOf(listOf("a", "b"))
        val animations = mutableMapOf<String, SpecRecordingAnimation>()
        val orchestrator = createTestOrchestrator(
            scope = backgroundScope,
            stack = stack,
            animations = animations,
        )
        runCurrent()
        advanceUntilIdle()
        assertTrue(orchestrator.itemsToRender.isNotEmpty())

        orchestrator.dispose()
        updateState(stack, listOf("c"))
        runCurrent()
        advanceUntilIdle()

        assertTrue(orchestrator.itemsToRender.isEmpty())
    }

    @Test
    fun givenDuplicateKeysOrEqualObjects_whenPushed_thenFailsFastWithDescriptiveError() {
        val duplicateHistory = com.nxoim.caif.prefabs.stack.StackHistory<String, String> { "same-key" }
        val err = assertFailsWith<IllegalArgumentException> {
            duplicateHistory.push(listOf("itemA", "itemB"))
        }
        assertTrue(err.message.orEmpty().contains("uniquely"), "Error must explain that keys must be unique")

        class ConflictingItem(val id: String) {
            override fun equals(other: Any?) = other is ConflictingItem
            override fun hashCode() = 0
        }
        val conflictingHistory = com.nxoim.caif.prefabs.stack.StackHistory<ConflictingItem, String> { it.id }
        val conflictErr = assertFailsWith<IllegalArgumentException> {
            conflictingHistory.push(listOf(ConflictingItem("1"), ConflictingItem("2")))
        }
        assertTrue(conflictErr.message.orEmpty().contains("equal"), "Error must explain equal items conflict")
    }

    @Test
    fun givenNegativeMaxAffected_whenConstructed_thenFailsFast() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val err = assertFailsWith<IllegalArgumentException> {
            StackOrchestrator(
                scope = scope,
                stack = mutableStateOf(emptyList()),
                registry = ItemAnimationRegistry { _, _ -> SpecRecordingAnimation() },
                resolver = specStackResolver,
                maxAffected = -1,
                renderOrder = RenderOrderStrategy.insertionOrder(),
            )
        }
        assertTrue(err.message.orEmpty().contains("maxAffected"))
        scope.coroutineContext[Job]?.cancel()
    }

    @Test
    fun givenMalformedRenderOrderStrategy_whenCycleRuns_thenValidatesExactKeySet() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        val invalidStrategy = StackOrchestrator(
            scope = scope,
            stack = mutableStateOf(emptyList()),
            registry = ItemAnimationRegistry { _, _ -> SpecRecordingAnimation() },
            resolver = specStackResolver,
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy { _, _ -> listOf("missing_key") },
        )
        assertFailsWith<IllegalArgumentException> {
            invalidStrategy.startCycle(listOf("real_key"))
        }
        scope.coroutineContext[Job]?.cancel()
    }
}

private fun <T> updateState(state: MutableState<T>, value: T) {
    Snapshot.withMutableSnapshot { state.value = value }
}

private fun StackOrchestrator<String, String, *, *>.renderedKeys(): Set<String> =
    itemsToRender.map { it.first.first }.toSet()

private val specStackContextFactory =
    ContextFactory<String, StackItemPosition, StackCreationContext<String>> { item ->
        when (intention) {
            AppearanceIntention.Entrance -> StackItemPosition.PreEntered
            AppearanceIntention.Removal -> StackItemPosition.Removed
            AppearanceIntention.Movement -> StackItemPosition.Inside(
                indexOf(item),
                previousIndexOf(item)
            )
        }
    }

private val specStackResolver: ContextResolver<String, String, StackItemPosition, StackCreationContext<String>> =
    defaultStackContextResolver(
        contextFactory = specStackContextFactory,
        keyFor = { it }
    )

private class SpecRecordingAnimation(
    private val removalDeferred: CompletableDeferred<Unit>? = null
) : ItemAnimation<StackItemPosition> {
    override val modifier: Modifier = Modifier
    val targets = mutableListOf<StackItemPosition>()

    override fun reset(context: StackItemPosition) = Unit

    override suspend fun animateTo(target: StackItemPosition) {
        targets += target
        if (target is StackItemPosition.Removed) {
            removalDeferred?.await()
        }
    }

    override fun willBeVisible(context: StackItemPosition): Boolean =
        context is StackItemPosition.Inside

    override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? = null
}

private fun createTestOrchestrator(
    scope: CoroutineScope,
    stack: MutableState<List<String>>,
    animations: MutableMap<String, SpecRecordingAnimation>,
    removalDeferred: Map<String, CompletableDeferred<Unit>> = emptyMap(),
    maxAffected: Int = Int.MAX_VALUE,
    policy: AffectedItemsPolicy<String, String, StackItemPosition> = AffectedItemsPolicy.fromTop(),
    renderOrder: RenderOrderStrategy<String> = RenderOrderStrategy.insertionOrder(),
): StackOrchestrator<String, String, StackItemPosition, StackCreationContext<String>> =
    StackOrchestrator(
        scope = scope,
        stack = stack,
        registry = ItemAnimationRegistry { _, key ->
            SpecRecordingAnimation(removalDeferred[key]).also { animations[key] = it }
        },
        resolver = specStackResolver,
        affectedItemsPolicy = policy,
        maxAffected = maxAffected,
        renderOrder = renderOrder,
    )
