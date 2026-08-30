@file:OptIn(ExperimentalTransitionApi::class)

package com.nxoim.sample.ui.common.sharedtransition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import com.nxoim.caif.prefabs.stack.LocalStackItemIsVisible
import com.nxoim.sample.ui.theme.fastSpring

val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope =
    staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

val LocalSharedElementsEnabled = compositionLocalOf { true }

/**
 * Sets up sample shared elements if the environment and scope permit it.
 */
@Composable
fun Modifier.sharedBounds(
    key: Any?,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
        ContentScale.FillWidth
    ),
    renderInOverlayDuringTransition: Boolean = true,
    boundsSpec: FiniteAnimationSpec<Rect> = fastSpring(visibilityThreshold = onePixelRect)
): Modifier {
    if (!LocalSharedElementsEnabled.current) return this
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current ?: return this
    key ?: return this
    val sharedContentState = sharedTransitionScope.rememberSharedContentState(key)

    // disable shared element transitions so on target change
    // the shared transition scope picks up on the changes.
    // this workaround is needed because we are not transitioning
    // from not composed state like we are supposed to.
    // the scope relies on something about the recomposition to
    // set the targets correctly
    if (!LocalStackItemIsVisible.current) return this

    return with(sharedTransitionScope) {
        sharedBounds(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = resizeMode,
            boundsTransform = { _, _ -> boundsSpec },
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
        )
    }
}

private val onePixelRect = Rect(1f, 1f, 1f, 1f)
