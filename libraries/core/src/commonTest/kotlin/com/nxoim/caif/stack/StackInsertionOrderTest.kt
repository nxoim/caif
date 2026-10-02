package com.nxoim.caif.stack

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.prefabs.stack.AppearanceIntention
import com.nxoim.caif.prefabs.stack.ContextFactory
import com.nxoim.caif.prefabs.stack.ItemAnimationRegistry
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import com.nxoim.caif.prefabs.stack.StackCreationContext
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.StackOrchestrator
import com.nxoim.caif.prefabs.stack.defaultStackContextResolver
import com.nxoim.caif.prefabs.stack.indexOf
import com.nxoim.caif.prefabs.stack.previousIndexOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StackInsertionOrderTest {
    @Test
    fun givenInsertionOrder_whenManyItemsAreMounted_thenRenderingFollowsJobInsertionOrder() = runTest {
        val stack = mutableStateOf(emptyList<String>())
        val orchestrator = orchestrator(backgroundScope, stack, emptyMap())
        runCurrent()

        val keys = (0 until 32).map { "page_$it" }
        Snapshot.withMutableSnapshot { stack.value = keys }
        runCurrent()

        assertEquals(keys.reversed(), orchestrator.itemsToRender.map { it.first.first })
    }

    @Test
    fun givenInsertionOrder_whenAddingAndRemovingItems_thenRenderingRetainsMountHistoryUntilExitCompletes() = runTest {
        val original = (0 until 8).map { "page_$it" }
        val added = listOf("new_a", "new_b")
        val stack = mutableStateOf(original)
        val exit = CompletableDeferred<Unit>()
        val removedKey = original[3]
        val orchestrator = orchestrator(backgroundScope, stack, mapOf(removedKey to exit))
        runCurrent()

        Snapshot.withMutableSnapshot { stack.value = original + added }
        runCurrent()
        val insertionHistory = original.reversed() + added.reversed()
        assertEquals(insertionHistory, orchestrator.itemsToRender.map { it.first.first })

        Snapshot.withMutableSnapshot { stack.value = original.filterNot { it == removedKey } + added }
        runCurrent()
        assertEquals(insertionHistory, orchestrator.itemsToRender.map { it.first.first })

        exit.complete(Unit)
        runCurrent()
        assertEquals(insertionHistory.filterNot { it == removedKey }, orchestrator.itemsToRender.map { it.first.first })
    }

    @Test
    fun givenPendingExitsOnUnconfinedScope_whenDisposed_thenAllJobsAreCancelledAndRenderingCleared() {
        val scopeJob = SupervisorJob()
        val scope = CoroutineScope(scopeJob + Dispatchers.Unconfined)
        val stack = mutableStateOf(listOf("first", "second"))
        val exits = mapOf(
            "first" to CompletableDeferred<Unit>(),
            "second" to CompletableDeferred<Unit>()
        )
        try {
            val orchestrator = orchestrator(scope, stack, exits)
            Snapshot.withMutableSnapshot { stack.value = emptyList() }
            orchestrator.startCycle(emptyList())
            orchestrator.progressCycle()
            assertEquals(2, orchestrator.itemsToRender.size)

            orchestrator.dispose()

            assertEquals(emptyList(), orchestrator.itemsToRender)
            assertEquals(0, scopeJob.children.count())
        } finally {
            scopeJob.cancel()
        }
    }

    private fun orchestrator(
        scope: CoroutineScope,
        stack: androidx.compose.runtime.State<List<String>>,
        exits: Map<String, CompletableDeferred<Unit>>
    ): StackOrchestrator<String, String, StackItemPosition, StackCreationContext<String>> =
        StackOrchestrator(
            scope = scope,
            stack = stack,
            registry = ItemAnimationRegistry { _, key -> ExitAwaitingAnimation(exits[key]) },
            resolver = defaultStackContextResolver(
                contextFactory = ContextFactory { item ->
                    when (intention) {
                        AppearanceIntention.Entrance -> StackItemPosition.PreEntered
                        AppearanceIntention.Removal -> StackItemPosition.Removed
                        AppearanceIntention.Movement -> StackItemPosition.Inside(indexOf(item), previousIndexOf(item))
                    }
                },
                keyFor = { it }
            ),
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy.insertionOrder()
        )

    private class ExitAwaitingAnimation(private val exit: CompletableDeferred<Unit>?) : ItemAnimation<StackItemPosition> {
        override val modifier = Modifier
        override fun reset(context: StackItemPosition) = Unit
        override suspend fun animateTo(target: StackItemPosition) {
            if (target is StackItemPosition.Removed) exit?.await()
        }
        override fun willBeVisible(context: StackItemPosition) = context is StackItemPosition.Inside
        override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? = null
    }
}
