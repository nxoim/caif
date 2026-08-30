package com.nxoim.caif.prefabs.stack

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

interface CapabilityDispatcher {
    val capabilityType: KClass<*>
}

@OptIn(ExperimentalAtomicApi::class)
abstract class BaseCapabilityDispatcher<T : Any>(
    override val capabilityType: KClass<T>,
    @PublishedApi internal val cycleController: StackCycleController
) : CapabilityDispatcher {
    @PublishedApi
    internal val state = AtomicReference(emptyState())

    fun startCycle(): Map<Any, T?> {
        val newAffectedItems = cycleController.startCycle(capabilityType)

        val newState = CycleState(
            cycleId = cycleController.currentCycleId,
            affectedItems = newAffectedItems
        )

        state.store(newState)
        return newAffectedItems
    }

    fun progressCycle() {
        val previousState = state.exchange(emptyState())
        val id = previousState.cycleId ?: return

        cycleController.progressCycle(id)
    }

    inline fun forEachAffectedItemsCapability(block: T.() -> Unit) {
        val snapshot = state.load()
        val id = snapshot.cycleId ?: return

        if (!cycleController.isCycleActive(id)) {
            // if startCycle published a newer state meanwhile, compareAndSet fails and preserves it
            state.compareAndSet(
                expectedValue = snapshot,
                newValue = emptyState()
            )
            return
        }

        snapshot.affectedItems.values.forEach { capability ->
            capability?.block()
        }
    }

    @PublishedApi
    internal fun emptyState(): CycleState<T> =
        CycleState(
            cycleId = null,
            affectedItems = emptyMap()
        )
}

@PublishedApi
internal data class CycleState<T : Any>(
    val cycleId: Long?,
    val affectedItems: Map<Any, T?>
)