package com.nxoim.caif.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory

fun interface DecomposeAnimationFactory<in Configuration : Any, in Child : Any> :
    ItemAnimationFactory<Child, Configuration, StackAnimationContext> {

    companion object {
        operator fun <Configuration : Any, Child : Any> invoke(
            defaultAnimation: () -> ItemAnimation<StackAnimationContext> = { adaptiveStackAnimation() },
            animationFor: (configuration: Configuration, child: Child) -> ItemAnimation<StackAnimationContext>? = { _, _ ->
                null
            },
        ): DecomposeAnimationFactory<Configuration, Child> =
            DecomposeAnimationFactory { child, config ->
                animationFor(config, child) ?: defaultAnimation()
            }
    }
}

/**
 * Creates a [DecomposeAnimationFactory] with a default animation for all items.
 */
fun <Configuration : Any, Child : Any> decomposeAnimations(
    default: () -> ItemAnimation<StackAnimationContext> = { adaptiveStackAnimation() },
): DecomposeAnimationFactory<Configuration, Child> =
    DecomposeAnimationFactory { _, _ -> default() }

/**
 * Creates an exhaustive [DecomposeAnimationFactory] mapping [Child] to animations.
 */
fun <Configuration : Any, Child : Any> decomposeAnimations(
    selector: (Child) -> ItemAnimation<StackAnimationContext>,
): DecomposeAnimationFactory<Configuration, Child> =
    DecomposeAnimationFactory { child, _ -> selector(child) }

/**
 * Creates an exhaustive [DecomposeAnimationFactory] mapping [Configuration] and [Child] to animations.
 */
fun <Configuration : Any, Child : Any> decomposeAnimations(
    selector: (Configuration, Child) -> ItemAnimation<StackAnimationContext>,
): DecomposeAnimationFactory<Configuration, Child> =
    DecomposeAnimationFactory { child, config -> selector(config, child) }

/**
 * Creates a [DecomposeAnimationFactory] with a customizable [default] fallback.
 */
fun <Configuration : Any, Child : Any> decomposeAnimations(
    default: () -> ItemAnimation<StackAnimationContext>,
    selector: (Child) -> ItemAnimation<StackAnimationContext>?,
): DecomposeAnimationFactory<Configuration, Child> =
    DecomposeAnimationFactory { child, _ ->
        selector(child) ?: default()
    }

/**
 * Creates a remembered [DecomposeAnimationFactory] mapping [Child] to animations.
 */
@Composable
fun <Configuration : Any, Child : Any> rememberDecomposeAnimations(
    selector: (Child) -> ItemAnimation<StackAnimationContext>,
): DecomposeAnimationFactory<Configuration, Child> {
    val currentSelector = rememberUpdatedState(selector)
    return remember {
        DecomposeAnimationFactory { child, _ ->
            currentSelector.value(child)
        }
    }
}

/**
 * Creates a remembered [DecomposeAnimationFactory] mapping [Configuration] and [Child] to animations.
 */
@Composable
fun <Configuration : Any, Child : Any> rememberDecomposeAnimations(
    selector: (Configuration, Child) -> ItemAnimation<StackAnimationContext>,
): DecomposeAnimationFactory<Configuration, Child> {
    val currentSelector = rememberUpdatedState(selector)
    return remember {
        DecomposeAnimationFactory { child, config ->
            currentSelector.value(config, child)
        }
    }
}

/**
 * Creates a remembered [DecomposeAnimationFactory] with a default animation for all items.
 */
@Composable
fun <Configuration : Any, Child : Any> rememberDecomposeAnimations(
    default: () -> ItemAnimation<StackAnimationContext> = { adaptiveStackAnimation() },
): DecomposeAnimationFactory<Configuration, Child> {
    val currentDefault = rememberUpdatedState(default)
    return remember {
        DecomposeAnimationFactory { _, _ -> currentDefault.value() }
    }
}

val LocalDecomposeAnimationFactory = staticCompositionLocalOf<DecomposeAnimationFactory<Any, Any>> {
    DefaultDecomposeAnimationFactoryInstance
}

private val DefaultDecomposeAnimationFactoryInstance = DecomposeAnimationFactory<Any, Any>(
    defaultAnimation = { adaptiveStackAnimation() },
)
