package com.nxoim.sample.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.caif.decompose.DecomposeStack
import com.nxoim.caif.decompose.adaptiveStackAnimation
import com.nxoim.caif.decompose.decomposeAnimations
import com.nxoim.sample.ui.board.BoardScreen
import com.nxoim.sample.ui.category.CategoryStackHost
import com.nxoim.sample.ui.common.FlowSharedElementKeys
import com.nxoim.sample.ui.common.expansionSwipeStackAnimation
import com.nxoim.sample.ui.common.inspector.DebugPopover
import com.nxoim.sample.ui.common.sharedtransition.LocalAnimatedVisibilityScope
import com.nxoim.sample.ui.common.sharedtransition.LocalSharedElementsEnabled
import com.nxoim.sample.ui.common.sharedtransition.LocalSharedTransitionScope
import com.nxoim.sample.ui.composer.TaskComposerStackHost
import com.nxoim.sample.ui.task.TaskDetailsStackHost

@Composable
internal fun FlowDemo(
    component: FlowComponent,
    sharedElementsEnabled: Boolean = LocalSharedElementsEnabled.current,
    onToggleSharedElements: (Boolean) -> Unit = {},
) {
    val useSharedElements = LocalSharedElementsEnabled.current

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
        ) {
            Box(Modifier.fillMaxSize()) {
                DecomposeStack(
                    stack = component.stack,
                    backHandler = component.backHandler,
                    onPop = component.navigation::navigateBack,
                    animationFactory = remember(useSharedElements) {
                        decomposeAnimations { child ->
                            when (child) {
                                is FlowChild.Board -> adaptiveStackAnimation()
                                is FlowChild.Category,
                                is FlowChild.TaskDetails,
                                is FlowChild.TaskComposer -> if (useSharedElements)
                                    expansionSwipeStackAnimation()
                                else
                                    adaptiveStackAnimation()
                            }
                        }
                    },
                ) { child ->
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        when (child) {
                            is FlowChild.Board -> BoardScreen(
                                sharedElementKeys = FlowSharedElementKeys,
                                controller = child.component.model,
                                onCategory = component.navigation::openCategory,
                                onTask = component.navigation::openTask,
                                onComposer = component.navigation::openTaskComposer,
                            )

                            is FlowChild.Category -> CategoryStackHost(
                                sharedElementKey = FlowSharedElementKeys.category(
                                    child.component.categoryId,
                                ),
                                sharedElementKeys = FlowSharedElementKeys,
                                component = child.component,
                            )

                            is FlowChild.TaskDetails -> TaskDetailsStackHost(
                                sharedElementKey = FlowSharedElementKeys.boardTask(
                                    child.component.model.taskId,
                                ),
                                sharedElementKeys = FlowSharedElementKeys,
                                component = child.component,
                                controller = child.component.model,
                            )

                            is FlowChild.TaskComposer -> TaskComposerStackHost(
                                sharedElementKey = FlowSharedElementKeys.composer(),
                                component = child.component,
                            )
                        }
                    }
                }

                DebugPopover(
                    sharedElementsEnabled = sharedElementsEnabled,
                    onToggleSharedElements = onToggleSharedElements,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(24.dp),
                )
            }
        }
    }
}
