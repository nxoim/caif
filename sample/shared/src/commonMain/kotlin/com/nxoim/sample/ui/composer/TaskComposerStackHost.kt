package com.nxoim.sample.ui.composer

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.nxoim.caif.decompose.DecomposeStack
import com.nxoim.caif.decompose.adaptiveStackAnimation
import com.nxoim.caif.decompose.rememberDecomposeAnimations
import com.nxoim.sample.ui.common.sharedtransition.LocalAnimatedVisibilityScope
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds

@Composable
internal fun TaskComposerStackHost(
    sharedElementKey: Any,
    component: TaskComposerComponent,
) {
    DecomposeStack(
        stack = component.stack,
        backHandler = component.backHandler,
        onPop = component.navigation::navigateBack,
        modifier = Modifier.sharedBounds(sharedElementKey),
        animationFactory = rememberDecomposeAnimations { child ->
            when (child) {
                ComposerChild.Writing,
                is ComposerChild.CategorySelection -> adaptiveStackAnimation()
            }
        },
    ) { child ->
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            when (child) {
                ComposerChild.Writing -> WritingScreen(
                    controller = component.model,
                    onBack = component.navigation::navigateBack,
                )

                is ComposerChild.CategorySelection -> CategorySelectionScreen(
                    controller = child.component.model,
                    onBack = component.navigation::navigateBack,
                )
            }
        }
    }
}
