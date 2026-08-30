package com.nxoim.caif.prefabs.stack

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastForEach


/**
 * Renders stack items with an [AnimatedVisibilityScope] derived from the topmost stack item.
 *
 * This layout automatically creates the root top-item transition, derives the keys retained by
 * [retentionPolicy], and exposes each item's [AnimatedVisibilityScope]. Nested stack animators
 * inherit the enclosing scope implicitly while their content is composed.
 */
@Composable
fun <ItemType : Any, Key : Any, Context> StackAnimatorLayout(
    animator: StackAnimatorState<ItemType, Key, Context>,
    modifier: Modifier = Modifier,
    retentionPolicy: StackTransitionRetentionPolicy<Key> = StackTransitionRetentionPolicy.adjacent(),
    parentAnimatedVisibilityScope: AnimatedVisibilityScope? = LocalStackAnimatedVisibilityScope.current,
    content: @Composable AnimatedVisibilityScope.(Key, ItemType) -> Unit
) {
    val stackKeys = animator.targetStackKeys
    val transition = updateTransition(
        targetState = stackKeys.firstOrNull(),
        label = "Topmost stack item"
    )
    val retainedKeys = retentionPolicy.retainedKeys(
        stack = stackKeys,
        transitionSource = transition.currentState
    )

    StackAnimatorLayout(
        animator = animator,
        transition = transition,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        modifier = modifier,
        retainWhileTransitionRunning = { key, _ -> key in retainedKeys },
    ) { key, type ->
        CompositionLocalProvider(LocalStackAnimatedVisibilityScope provides this) {
            content(key, type)
        }
    }
}

/**
 * Renders stack items with an [AnimatedVisibilityScope] derived from [transition].
 *
 * [transition] must target the key of the current topmost item, or `null` for an empty stack. Each
 * rendered item receives a child transition that is visible only while it is the topmost item.
 * Items that have not yet been topmost use [EnterExitState.PreEnter]; items that were previously
 * topmost use [EnterExitState.PostExit].
 *
 * [retainWhileTransitionRunning] controls whether an item's Compose transition participates in its
 * render lifetime. Items excluded by that policy complete according to their item animation alone.
 * The policy does not control whether [content] can use the transition; every item receives an
 * [AnimatedVisibilityScope].
 *
 * A nested stack should inherit the current item's scope implicitly through the composition. Its child
 * transition follows the nested stack while the parent is visible, and follows the parent state
 * while the parent is entering or leaving composition.
 *
 * Transition states are matched to rendered items by [Key].
 */
@OptIn(ExperimentalTransitionApi::class)
@Composable
fun <ItemType, Key : Any, Context> StackAnimatorLayout(
    animator: StackAnimatorState<ItemType, Key, Context>,
    transition: Transition<Key?>,
    parentAnimatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
    retainWhileTransitionRunning: (Key, ItemType) -> Boolean = { _, _ -> true },
    content: @Composable AnimatedVisibilityScope.(Key, ItemType) -> Unit
) {
    animator.itemsToRender.fastForEach { (pair, animation) ->
        val itemKey = pair.first
        val item = pair.second

        key(itemKey) {
            val animatedVisibilityTransition = transition.createChildTransition(
                label = "Stack item $itemKey visibility"
            ) { topmostItemKey ->
                transition.targetEnterExit(
                    itemKey = itemKey,
                    targetState = topmostItemKey,
                    parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
                )
            }
            val animatedVisibilityScope = remember(animatedVisibilityTransition) {
                AnimatedVisibilityScopeImpl(
                    animatedVisibilityTransition
                )
            }
            val parentIsVisible = LocalStackItemIsVisible.current
            val isItemVisible = parentIsVisible && transition.isItemVisible(itemKey)
            val shouldRetainForTransition = retainWhileTransitionRunning(itemKey, item)

            DisposableEffect(
                animator,
                itemKey,
                animatedVisibilityTransition,
                shouldRetainForTransition
            ) {
                // when a screen is popped, navigation should remove it
                // from state immediately. we register the compose
                // transition as an external animation, so the orchestrator
                // retains the outgoing item in composition UNTIL child
                // transitions, and shared element bounds, have
                // completed their handoff
                val registration = if (shouldRetainForTransition) {
                    animator.registerExternalAnimation(itemKey) {
                        animatedVisibilityTransition.isRunning ||
                                animatedVisibilityTransition.currentState !=
                                animatedVisibilityTransition.targetState ||
                                (transition.currentState == itemKey &&
                                        transition.targetState != itemKey)
                    }
                } else {
                    null
                }
                onDispose {
                    registration?.unregister()
                }
            }
            Layout(
                content = {
                    CompositionLocalProvider(LocalStackItemIsVisible provides isItemVisible) {
                        content(animatedVisibilityScope, itemKey, item)
                    }
                },
                modifier = modifier.then(animation.modifier)
            ) { measurables, constraints ->
                val placeable = measurables.firstOrNull()?.measure(constraints)

                placeable ?: return@Layout layout(0, 0) { }

                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }
    }
}

/**
 * Selects the stack keys whose external transitions participate in render retention.
 *
 * Implementations should return keys from stack and may additionally return
 * transitionSource when it represents an item leaving the stack.
 */
fun interface StackTransitionRetentionPolicy<Key : Any> {
    fun retainedKeys(
        stack: List<Key>,
        transitionSource: Key?
    ): Set<Key>

    companion object {
        /**
         * Retains the top item, the item beneath it, and the latest removed transition source.
         */
        fun <Key : Any> adjacent(): StackTransitionRetentionPolicy<Key> =
            StackTransitionRetentionPolicy { stack, transitionSource ->
                buildSet(3) {
                    stack.firstOrNull()?.let(::add)
                    stack.getOrNull(1)?.let(::add)
                    if (transitionSource != null && transitionSource !in stack) {
                        add(transitionSource)
                    }
                }
            }
    }
}

private class AnimatedVisibilityScopeImpl(
    override val transition: Transition<EnterExitState>,
) : AnimatedVisibilityScope

@OptIn(ExperimentalDeferredTransitionApi::class)
private fun <Key : Any> Transition<Key?>.isItemVisible(itemKey: Key): Boolean =
    currentState == itemKey ||
            targetState == itemKey ||
            pendingTargetState == itemKey ||
            isSeeking

// mirrors AnimatedVisibility's conversion from an arbitrary state to EnterExitState
@OptIn(ExperimentalDeferredTransitionApi::class)
@Composable
private fun <Key : Any> Transition<Key?>.targetEnterExit(
    itemKey: Key,
    targetState: Key?,
    parentAnimatedVisibilityScope: AnimatedVisibilityScope?,
): EnterExitState = key(this, itemKey) {
    val childState = if (isSeeking) {
        if (targetState == itemKey) {
            EnterExitState.Visible
        } else if (currentState == itemKey) {
            EnterExitState.PostExit
        } else {
            EnterExitState.PreEnter
        }
    } else {
        val hasBeenVisible = remember { mutableStateOf(false) }
        val pendingTargetState = pendingTargetState

        if (currentState == itemKey || pendingTargetState == itemKey) {
            hasBeenVisible.value = true
        }

        when {
            targetState == itemKey -> EnterExitState.Visible
            pendingTargetState == itemKey -> EnterExitState.PreEnter
            hasBeenVisible.value -> EnterExitState.PostExit
            else -> EnterExitState.PreEnter
        }
    }

    return@key parentAnimatedVisibilityScope
        ?.let { parentScope ->
            val parentTransition = parentScope.transition
            val parentDestinationIsVisible =
                parentTransition.targetState == EnterExitState.Visible ||
                        parentTransition.pendingTargetState == EnterExitState.Visible
            if (parentDestinationIsVisible)
                childState
            else
                parentTransition.targetState
        }
        ?: childState
}

/**
 * Nested stack animators inherit this scope to coordinate enter/exit transitions.
 */
val LocalStackAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Whether the current stack item and all of its enclosing stack items participate in their
 * visibility transitions. Content remains composed when this is false.
 */
val LocalStackItemIsVisible = compositionLocalOf { true }