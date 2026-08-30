package com.nxoim.caif.prefabs.stack

import kotlin.reflect.KClass

interface StackCycleController {
    /**
     * Starts a new capability input cycle. Any previously open input cycle becomes stale.
     * Returns an immutable map instance.
     */
    fun <T : Any> startCycle(kClass: KClass<T>): Map<Any, T?>
    val currentCycleId: Long get() = 0L
    fun isCycleActive(id: Long): Boolean
    fun progressCycle()
    fun progressCycle(id: Long) {
        if (isCycleActive(id)) progressCycle()
    }
}
