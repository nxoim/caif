package com.nxoim.caif.stack

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.nxoim.caif.core.BaseItemAnimation
import com.nxoim.caif.prefabs.stack.AffectedItemsPolicy
import com.nxoim.caif.prefabs.stack.StackAnimatorState
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.StackOrder
import com.nxoim.caif.prefabs.stack.StackTransitionRetentionPolicy
import com.nxoim.caif.prefabs.stack.rememberStackAnimatorState
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
