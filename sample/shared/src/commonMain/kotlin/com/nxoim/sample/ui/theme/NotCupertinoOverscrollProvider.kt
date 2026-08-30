package com.nxoim.sample.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.nxoim.caif.springs.springB

@Composable
fun rememberNotCupertinoOverscrollFactory(
    animationSpec: AnimationSpec<Float> = springB(
        response = 0.4f,
        dampingFraction = 1.05f
        // visibility threshold is enforced in the overscroll effect
    )
): NotCupertinoOverscrollEffectFactory {
    val layoutDirection = LocalLayoutDirection.current

    return remember {
        NotCupertinoOverscrollEffectFactory(
            layoutDirection = layoutDirection,
            animationSpec = animationSpec
        )
    }
}

data class NotCupertinoOverscrollEffectFactory(
    private val layoutDirection: LayoutDirection,
    private val animationSpec: AnimationSpec<Float>
) : OverscrollFactory {
    @OptIn(ExperimentalFoundationApi::class)
    override fun createOverscrollEffect() =
        NotCupertinoOverscrollEffect(
            applyClip = false,
            animationSpec = animationSpec
        )
}