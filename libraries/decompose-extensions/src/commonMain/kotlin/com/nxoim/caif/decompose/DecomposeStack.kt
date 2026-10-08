@file:OptIn(ExperimentalTransitionApi::class)

package com.nxoim.caif.decompose

import androidx.collection.MutableScatterSet
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import com.arkivanov.decompose.Child.Created
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import com.nxoim.caif.prefabs.stack.StackAnimatorLayout
import com.nxoim.caif.prefabs.stack.StackOrder
import com.nxoim.caif.prefabs.stack.getOrCreateDispatcher
import com.nxoim.caif.prefabs.stack.rememberStackAnimatorState
import com.nxoim.caif.swipeable.SwipeConstraint
import com.nxoim.caif.swipeable.swipeable

@Composable
fun <Configuration : Any, Child : Any> DecomposeStack(
    stack: Value<ChildStack<Configuration, Child>>,
    onPop: () -> Unit,
    modifier: Modifier = Modifier,
    backHandler: BackHandler? = null,
    animationFactory: DecomposeAnimationFactory<Configuration, Child>? = null,
    enableGestures: Boolean = true,
    content: @Composable AnimatedVisibilityScope.(Child) -> Unit,
) {
    val resolvedAnimationFactory = resolveDecomposeAnimationFactory(animationFactory)
    val fallbackForChildren = resolvedAnimationFactory.fallback ?: LocalDecomposeAnimationFallback.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val stackState = rememberStackItems(stack) { it.items }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val animationEnvironment = rememberStackAnimationEnvironmentState()
        val parentSwipeGesture = LocalParentBackGesture.current
        val animator = rememberStackAnimatorState(
            stack = stackState,
            stackOrder = StackOrder.TopmostLast,
            keyFor = { it.configuration },
            factory = resolvedAnimationFactory,
            contextFactory = remember(animationEnvironment) {
                stackAnimationContextFactory { animationEnvironment.value }
            },
        )

        val swipeDispatcher = animator.getOrCreateDispatcher(::SwipeCapabilityDispatcher)
        val predictiveBackDispatcher =
            animator.getOrCreateDispatcher(::PredictiveBackCapabilityDispatcher)
        val currentOnPop = rememberUpdatedState(onPop)

        val swipeGesture = remember(
            parentSwipeGesture,
            swipeDispatcher,
        ) {
            StackGestureParticipant(
                canPop = { stackState.value.size > 1 },
                parent = parentSwipeGesture,
                startLocal = swipeDispatcher::onStart,
                updateLocal = { delta, _, _ ->
                    swipeDispatcher.onUpdate(delta)
                },
                confirmLocal = { velocity ->
                    if (stackState.value.size > 1) {
                        currentOnPop.value()
                    }
                    swipeDispatcher.onEnd(velocity)
                },
                cancelLocal = { velocity ->
                    swipeDispatcher.onEnd(velocity)
                },
            )
        }

        backHandler?.let {
            StackBackHandler(
                backHandler = it,
                canPop = stackState.value.size > 1,
                predictiveBackDispatcher = predictiveBackDispatcher,
                onPop = { onPop() },
            )
        }

        val swipeModifier = if (enableGestures) {
            Modifier.swipeable(
                detectionConstraint = SwipeConstraint.end(),
                willProcess = { swipeGesture.canHandle() },
                onStart = { swipeGesture.onStart() },
                onProgress = { delta, uptimeMillis, direction ->
                    swipeGesture.onProgress(delta, uptimeMillis, direction)
                },
                onConfirm = { velocity, _ -> swipeGesture.onConfirm(velocity) },
                onCancel = { velocity -> swipeGesture.onCancel(velocity) },
            )
        } else {
            Modifier
        }

        Box(
            Modifier
                .fillMaxSize()
                .decomposeKeyBackHandler(
                    canPop = stackState.value.size > 1,
                    onPop = onPop,
                    predictiveBackDispatcher = predictiveBackDispatcher,
                )
                .focusable()
                .then(swipeModifier)
        ) {
            RetainCompositionsEffect(
                saveableStateHolder,
                stackState,
            )

            StackAnimatorLayout(animator) { _, child ->
                saveableStateHolder.SaveableStateProvider(child.key) {
                    CompositionLocalProvider(
                        LocalParentBackGesture provides swipeGesture,
                        LocalDecomposeAnimationFallback provides fallbackForChildren,
                    ) {
                        content(child.instance)
                    }
                }
            }
        }
    }
}

@Composable
internal fun <Configuration : Any, Child : Any, Item : Any> rememberStackItems(
    stack: Value<ChildStack<Configuration, Child>>,
    items: (ChildStack<Configuration, Child>) -> List<Item>,
): State<List<Item>> {
    val currentItems = rememberUpdatedState(items)
    val state = remember(stack) {
        mutableStateOf(items(stack.value))
    }

    DisposableEffect(stack) {
        val subscription = stack.subscribe { state.value = currentItems.value(it) }
        onDispose(subscription::cancel)
    }

    return state
}

@Composable
@NonRestartableComposable
private fun <Child : Any, Configuration : Any> RetainCompositionsEffect(
    saveableStateHolder: SaveableStateHolder,
    stackState: State<List<Created<Configuration, Child>>>,
) {
    val keys = remember(saveableStateHolder) {
        Keys(stackState.value.mapKeys())
    }

    DisposableEffect(saveableStateHolder, stackState.value) {
        val currentKeys = stackState.value.mapKeys()

        keys.set.forEach {
            if (it !in currentKeys) {
                saveableStateHolder.removeState(it)
            }
        }

        keys.set = currentKeys

        onDispose {}
    }
}

private fun List<Created<*, *>>.mapKeys(): Set<String> {
    val set = MutableScatterSet<String>(size)
    fastForEach { set.add(it.key) }
    return set.asSet()
}

private class Keys(
    var set: Set<String>,
)
