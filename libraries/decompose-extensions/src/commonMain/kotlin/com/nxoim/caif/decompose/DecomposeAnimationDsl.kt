package com.nxoim.caif.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.arkivanov.decompose.Child.Created
import com.nxoim.caif.core.AnimationDeclarations
import com.nxoim.caif.core.AnimationSelectionStrategy
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory
import com.nxoim.caif.core.buildAnimationFactory
import com.nxoim.caif.core.fromItem

@RememberInComposition
fun <Configuration : Any, Child : Any> decomposeStackAnimations(
    swipe: ((Configuration, Child) -> ItemAnimation<StackAnimationContext>)? = null,
    predictiveBack: ((Configuration, Child) -> ItemAnimation<StackAnimationContext>)? = null,
    fallback: AnimationDeclarations<StackAnimationContext>.() -> Unit = DefaultDecomposeAnimationFallback,
    selectionStrategy: AnimationSelectionStrategy<Created<Configuration, Child>> = fromTopmost(),
): DecomposeAnimationFactory<Configuration, Child> = decomposeStackAnimations(selectionStrategy, fallback) { configuration, child ->
    if (swipe != null) {
        val swipeDefinition = onInput<SwipeCapability> { swipe(configuration, child) }
        defaultAnimation(swipeDefinition)
    }

    if (predictiveBack != null) {
        onInput<PredictiveBackCapability> { predictiveBack(configuration, child) }
    }
}

@RememberInComposition
fun <Configuration : Any, Child : Any> decomposeStackAnimations(
    selectionStrategy: AnimationSelectionStrategy<Created<Configuration, Child>> = fromTopmost(),
    fallback: AnimationDeclarations<StackAnimationContext>.() -> Unit = DefaultDecomposeAnimationFallback,
    declarations: AnimationDeclarations<StackAnimationContext>.(Configuration, Child) -> Unit,
): DecomposeAnimationFactory<Configuration, Child> = buildAnimationFactory(selectionStrategy, fallback) { entry, _ ->
    declarations(entry.configuration, entry.instance)
}

typealias DecomposeAnimationFactory<Configuration, Child> =
    ItemAnimationFactory<Created<Configuration, Child>, Configuration, StackAnimationContext>

/**
 * Snapshots are topmost-last
 */
@Suppress("UNCHECKED_CAST")
fun <Item : Any> fromTopmost(): AnimationSelectionStrategy<Item> =
    TopmostSelection as AnimationSelectionStrategy<Item>

val LocalDecomposeAnimationFallback = staticCompositionLocalOf<AnimationDeclarations<StackAnimationContext>.() -> Unit> {
    DefaultDecomposeAnimationFallback
}

@Composable
internal fun <Configuration : Any, Child : Any> resolveDecomposeAnimationFactory(
    explicitFactory: DecomposeAnimationFactory<Configuration, Child>? = null,
): DecomposeAnimationFactory<Configuration, Child> {
    if (explicitFactory != null) return explicitFactory
    val fallback = LocalDecomposeAnimationFallback.current
    return remember(fallback) { decomposeStackAnimations(fallback = fallback) }
}

private val TopmostSelection = fromItem<Any> { it.last() }

private val DefaultDecomposeAnimationFallback: AnimationDeclarations<StackAnimationContext>.() -> Unit = {
    val swipe = onInput<SwipeCapability> { CupertinoStackAnimation() }
    onInput<PredictiveBackCapability> { MaterialStackAnimation() }
    defaultAnimation(swipe)
}
