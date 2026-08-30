package com.nxoim.caif.core

import com.nxoim.caif.prefabs.stack.BaseCapabilityDispatcher
import com.nxoim.caif.prefabs.stack.StackCycleController
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalAtomicApi::class)
class CapabilityDispatcherTest {

    private class TestCapability {
        var callCount = 0
    }

    private class FakeCycleController : StackCycleController {
        override var currentCycleId: Long = 1L
        private val activeCycleIds = mutableSetOf<Long>()
        val capabilities = mutableMapOf<Any, TestCapability>()

        override fun <T : Any> startCycle(kClass: KClass<T>): Map<Any, T?> {
            currentCycleId++
            activeCycleIds.add(currentCycleId)
            @Suppress("UNCHECKED_CAST")
            return capabilities as Map<Any, T?>
        }

        override fun progressCycle() {
            activeCycleIds.remove(currentCycleId)
        }

        override fun progressCycle(id: Long) {
            activeCycleIds.remove(id)
        }

        override fun isCycleActive(id: Long): Boolean =
            id in activeCycleIds
    }

    private class TestDispatcher(
        controller: StackCycleController
    ) : BaseCapabilityDispatcher<TestCapability>(TestCapability::class, controller)

    @Test
    fun givenActiveCycle_whenDispatching_thenInvokesCapabilityForEveryAffectedItem() {
        val controller = FakeCycleController()
        val capA = TestCapability()
        val capB = TestCapability()
        controller.capabilities["a"] = capA
        controller.capabilities["b"] = capB

        val dispatcher = TestDispatcher(controller)
        val affected = dispatcher.startCycle()

        assertEquals(2, affected.size)

        dispatcher.forEachAffectedItemsCapability {
            callCount++
        }

        assertEquals(1, capA.callCount)
        assertEquals(1, capB.callCount)
    }

    @Test
    fun givenNewCycleStarted_whenStaleDispatcherInvoked_thenAtomicallyInvalidatesAndNoOps() {
        val controller = FakeCycleController()
        val capA = TestCapability()
        controller.capabilities["item"] = capA

        val firstDispatcher = TestDispatcher(controller)
        val secondDispatcher = TestDispatcher(controller)

        firstDispatcher.startCycle()
        val firstCycleId = controller.currentCycleId

        // Second cycle begins, making first cycle inactive in controller
        controller.progressCycle(firstCycleId)
        secondDispatcher.startCycle()

        // Calling dispatch on first dispatcher must detect inactive cycle and no-op
        firstDispatcher.forEachAffectedItemsCapability {
            callCount++
        }

        assertEquals(0, capA.callCount)
    }

    @Test
    fun givenCycleProgressed_thenStateResetsToEmpty() {
        val controller = FakeCycleController()
        val cap = TestCapability()
        controller.capabilities["x"] = cap

        val dispatcher = TestDispatcher(controller)
        dispatcher.startCycle()
        assertTrue(dispatcher.state.load().cycleId != null)

        dispatcher.progressCycle()
        assertEquals(null, dispatcher.state.load().cycleId)
        assertTrue(dispatcher.state.load().affectedItems.isEmpty())
    }
}
