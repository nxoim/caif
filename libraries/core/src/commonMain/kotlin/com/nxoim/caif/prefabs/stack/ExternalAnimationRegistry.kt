package com.nxoim.caif.prefabs.stack

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

/** A scoped registration for an animation that participates in stack item retention. */
fun interface ExternalAnimationRegistration {
    fun unregister()
}

internal class ExternalAnimationRegistry<Key : Any> {
    private val animations = mutableStateMapOf<Any, com.nxoim.caif.prefabs.stack.RegisteredExternalAnimation<Key>>()

    fun register(
        key: Key,
        isRunning: () -> Boolean
    ): com.nxoim.caif.prefabs.stack.ExternalAnimationRegistration {
        val registrationKey = Any()
        animations[registrationKey] =
            com.nxoim.caif.prefabs.stack.RegisteredExternalAnimation(
                key,
                isRunning
            )

        return com.nxoim.caif.prefabs.stack.ExternalAnimationRegistration {
            animations.remove(registrationKey)
        }
    }

    suspend fun awaitIdle(key: Key) {
        snapshotFlow {
            animations.values.none { animation ->
                animation.key == key && animation.isRunning()
            }
        }.first { isIdle -> isIdle }
    }

    fun clear() {
        animations.clear()
    }
}

private class RegisteredExternalAnimation<Key : Any>(
    val key: Key,
    val isRunning: () -> Boolean
)
