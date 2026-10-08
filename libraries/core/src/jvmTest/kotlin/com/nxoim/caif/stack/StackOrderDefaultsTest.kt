package com.nxoim.caif.stack

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory
import com.nxoim.caif.core.buildAnimationFactory
import com.nxoim.caif.core.fromItem
import com.nxoim.caif.prefabs.stack.AffectedItemsPolicy
import com.nxoim.caif.prefabs.stack.BaseCapabilityDispatcher
import com.nxoim.caif.prefabs.stack.StackAnimatorState
import com.nxoim.caif.prefabs.stack.StackCycleController
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.StackOrder
import com.nxoim.caif.prefabs.stack.StackTransitionRetentionPolicy
import com.nxoim.caif.prefabs.stack.rememberStackAnimatorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StackOrderDefaultsTest {
    @Test
    fun givenFactoryReplacement_whenRecomposed_thenEqualConfigurationIsRetainedAndChangesRebindNextCycle() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(backgroundScope.coroutineContext + clock)
        val composition = Composition(NoNodesApplier(), recomposer)
        val stack = mutableStateOf(listOf("base", "details"))
        // Publish distinct equal values so both retention and changed configuration are exercised.
        val factory = mutableStateOf(OriginFactory("old"), referentialEqualityPolicy())
        lateinit var animator: StackAnimatorState<String, String, StackItemPosition>
        val recompositionJob = backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition.setContent {
                FactoryAnimator(stack, factory.value, backgroundScope) { animator = it }
            }
            runCurrent()
            val owner = animator
            val slot = animator.itemsToRender.first().second
            val dispatcher = animator.getOrCreateDispatcher(OriginDispatcher::class) { OriginDispatcher(it) }
            val old = dispatcher.startCycle().getValue("base")!!
            Snapshot.withMutableSnapshot { factory.value = OriginFactory("old") }
            runCurrent()
            clock.sendFrame(0L)
            runCurrent()
            assertSame(old, dispatcher.startCycle().getValue("base"))

            Snapshot.withMutableSnapshot { factory.value = OriginFactory("new") }
            runCurrent()
            clock.sendFrame(1L)
            runCurrent()
            assertSame(owner, animator)
            assertSame(slot, animator.itemsToRender.first().second)
            dispatcher.progressCycle()
            runCurrent()
            assertSame(old, slot.getAndSelectCapability(OriginCapability::class))
            val next = dispatcher.startCycle().getValue("base")!!
            assertEquals("new", next.origin)
            assertSame(next, dispatcher.startCycle().getValue("base"))
        } finally {
            composition.dispose()
            recomposer.close()
            recompositionJob.cancel()
        }
    }

    @Test
    fun givenDeclaredFactory_whenRememberedAnimatorWrapsIt_thenCommonSourceSelectionIsPreserved() = runTest {
        val recomposer = Recomposer(backgroundScope.coroutineContext)
        val composition = Composition(NoNodesApplier(), recomposer)
        val stack = mutableStateOf(listOf("base", "details"))
        val factory = buildAnimationFactory<String, String, StackItemPosition>(fromItem { it.last() }) { item, _ ->
            defaultAnimation { OriginAnimation(item) }
        }
        lateinit var animator: StackAnimatorState<String, String, StackItemPosition>
        try {
            composition.setContent {
                animator = rememberStackAnimatorState(
                    stack = stack,
                    keyFor = { it },
                    factory = factory,
                    scope = backgroundScope,
                )
            }
            runCurrent()
            advanceUntilIdle()
            assertEquals(setOf("details"), animator.itemsToRender.map {
                it.second.getAndSelectCapability(OriginCapability::class)!!.origin
            }.toSet())
        } finally {
            composition.dispose()
            recomposer.close()
        }
    }

    @Test
    fun givenExplicitOrDefaultOrder_whenAnimatorUsesDefaults_thenParticipationRenderingAndRetentionAgree() = runTest {
        val cases = listOf(
            null to listOf("note", "task", "category", "home"),
            StackOrder.TopmostFirst to listOf("home", "category", "task", "note"),
            StackOrder.TopmostLast to listOf("note", "task", "category", "home"),
        )

        for ((order, items) in cases) {
            val recomposer = Recomposer(backgroundScope.coroutineContext)
            val composition = Composition(NoNodesApplier(), recomposer)
            val stack = mutableStateOf(items)
            val animations = mutableMapOf<String, VisiblePositionAnimation>()
            lateinit var animator: StackAnimatorState<String, String, StackItemPosition>

            try {
                composition.setContent {
                    animator = if (order == null) {
                        rememberStackAnimatorState(
                            stack = stack,
                            keyFor = { it },
                            factory = { item, _ ->
                                VisiblePositionAnimation().also { animations[item] = it }
                            },
                            maxAffected = 2,
                            scope = backgroundScope,
                        )
                    } else {
                        rememberStackAnimatorState(
                            stack = stack,
                            keyFor = { it },
                            factory = { item, _ ->
                                VisiblePositionAnimation().also { animations[item] = it }
                            },
                            stackOrder = order,
                            maxAffected = 2,
                            scope = backgroundScope,
                        )
                    }
                }
                runCurrent()
                advanceUntilIdle()

                assertEquals(order ?: StackOrder.TopmostLast, animator.stackOrder)
                assertEquals("home", animator.stackOrder.topmostItem(animator.targetStackKeys))
                assertEquals(listOf("category", "home"), animator.itemsToRender.map { it.first.first })
                val retentionPolicy = if (order == null)
                    StackTransitionRetentionPolicy.adjacent<String>()
                else
                    StackTransitionRetentionPolicy.adjacent<String>(animator.stackOrder)
                assertEquals(
                    setOf("home", "category"),
                    retentionPolicy.retainedKeys(animator.targetStackKeys, null),
                )

                val pushedItems = when (animator.stackOrder) {
                    StackOrder.TopmostFirst -> listOf("next") + items
                    StackOrder.TopmostLast -> items + "next"
                }
                Snapshot.withMutableSnapshot { stack.value = pushedItems }
                runCurrent()
                advanceUntilIdle()

                assertEquals(StackItemPosition.Inside(1, 0), animations.getValue("home").position)
                assertEquals(StackItemPosition.Inside(0, null), animations.getValue("next").position)

                Snapshot.withMutableSnapshot { stack.value = items }
                runCurrent()
                advanceUntilIdle()

                assertEquals(StackItemPosition.Inside(0, 1), animations.getValue("home").position)
            } finally {
                composition.dispose()
                recomposer.close()
            }
        }
    }

    @Test
    fun givenRememberedAnimator_whenParticipationPolicyChanges_thenNextCycleUsesReplacement() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(backgroundScope.coroutineContext + clock)
        val composition = Composition(NoNodesApplier(), recomposer)
        val stack = mutableStateOf(listOf("home"))
        val defaultPolicy = AffectedItemsPolicy.fromTop<String, String, StackItemPosition>()
        val policy = mutableStateOf(defaultPolicy)
        var replacementCalls = 0
        val replacement = AffectedItemsPolicy<String, String, StackItemPosition> {
                items, keys, isVisible, current, target, previous, maxAffected, minAffected ->
            replacementCalls++
            defaultPolicy.selectAffectedItems(
                items, keys, isVisible, current, target, previous, maxAffected, minAffected,
            )
        }
        lateinit var animator: StackAnimatorState<String, String, StackItemPosition>
        val recompositionJob = backgroundScope.launch(clock) {
            recomposer.runRecomposeAndApplyChanges()
        }

        try {
            composition.setContent {
                animator = rememberStackAnimatorState(
                    stack = stack,
                    keyFor = { it },
                    factory = { _, _ -> VisiblePositionAnimation() },
                    affectedItemsPolicy = policy.value,
                    scope = backgroundScope,
                )
            }
            runCurrent()
            advanceUntilIdle()
            val originalAnimator = animator

            Snapshot.withMutableSnapshot { policy.value = replacement }
            runCurrent()
            clock.sendFrame(0L)
            runCurrent()
            assertSame(originalAnimator, animator)

            Snapshot.withMutableSnapshot { stack.value = listOf("home", "next") }
            runCurrent()
            advanceUntilIdle()

            assertTrue(replacementCalls > 0, "The next cycle must consult the updated policy")
        } finally {
            composition.dispose()
            recomposer.close()
            recompositionJob.cancel()
        }
    }
}

@Composable
private fun FactoryAnimator(
    stack: State<List<String>>,
    factory: ItemAnimationFactory<String, String, StackItemPosition>,
    scope: CoroutineScope,
    onAnimator: (StackAnimatorState<String, String, StackItemPosition>) -> Unit
) {
    onAnimator(rememberStackAnimatorState(stack, keyFor = { it }, factory = factory, scope = scope))
}

private data class OriginFactory(private val origin: String) : ItemAnimationFactory<String, String, StackItemPosition> {
    override fun create(item: String, key: String): ItemAnimation<StackItemPosition> = OriginAnimation(origin)
}

private class OriginDispatcher(controller: StackCycleController) :
    BaseCapabilityDispatcher<OriginCapability>(OriginCapability::class, controller)

private interface OriginCapability { val origin: String }

private class OriginAnimation(override val origin: String) :
    BaseItemAnimation<StackItemPosition>(), OriginCapability {
    override val modifier = Modifier
    override fun StackItemPosition.isVisibleWhen(): Boolean = this is StackItemPosition.Inside
}

private class VisiblePositionAnimation : BaseItemAnimation<StackItemPosition>() {
    val position get() = currentContext
    override val modifier = Modifier
    override fun StackItemPosition.isVisibleWhen(): Boolean = this is StackItemPosition.Inside
}

private class NoNodesApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = error("Unexpected UI node")
    override fun insertBottomUp(index: Int, instance: Unit) = error("Unexpected UI node")
    override fun remove(index: Int, count: Int) = error("Unexpected UI node")
    override fun move(from: Int, to: Int, count: Int) = error("Unexpected UI node")
    override fun onClear() = Unit
}
