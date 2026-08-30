@file:OptIn(ExperimentalAnimationApi::class)

package com.nxoim.sample.ui.category

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxoim.caif.decompose.DecomposeStack
import com.nxoim.caif.decompose.adaptiveStackAnimation
import com.nxoim.caif.decompose.decomposeAnimations
import com.nxoim.sample.ui.common.ErrorState
import com.nxoim.sample.ui.common.LoadState
import com.nxoim.sample.ui.common.LoadingState
import com.nxoim.sample.ui.common.NotFoundState
import com.nxoim.sample.ui.common.SharedElementKeyFactory
import com.nxoim.sample.ui.common.expansionSwipeStackAnimation
import com.nxoim.sample.ui.common.sharedtransition.LocalAnimatedVisibilityScope
import com.nxoim.sample.ui.common.sharedtransition.LocalSharedElementsEnabled
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds
import com.nxoim.sample.ui.review.ReviewScreen
import com.nxoim.sample.ui.task.TaskDetailsStackHost
import com.nxoim.sample.ui.tasks.TaskListScreen

@Composable
internal fun CategoryStackHost(
    sharedElementKey: Any,
    sharedElementKeys: SharedElementKeyFactory,
    component: CategoryComponent,
) {
    val categoryState by component.model.category.collectAsState()
    val useSharedElements = LocalSharedElementsEnabled.current

    updateTransition(categoryState, label = "CategoryStateTransition").Crossfade(
        contentKey = { it::class },
    ) { state ->
        when (state) {
            LoadState.Loading -> LoadingState()
            LoadState.NotFound -> NotFoundState(entity = "Category")
            is LoadState.Error -> ErrorState(entity = "category", cause = state.cause)
            is LoadState.Content -> {
                val category = state.value
                DecomposeStack(
                    stack = component.stack,
                    backHandler = component.backHandler,
                    onPop = component.navigation::navigateBack,
                    modifier = Modifier.sharedBounds(sharedElementKey),
                    animationFactory = remember(useSharedElements) {
                        decomposeAnimations { child ->
                            when (child) {
                                CategoryChild.TaskList,
                                is CategoryChild.Review -> adaptiveStackAnimation()
                                is CategoryChild.TaskDetails -> if (useSharedElements)
                                    expansionSwipeStackAnimation()
                                else
                                    adaptiveStackAnimation()
                            }
                        }
                    },
                ) { child ->
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        when (child) {
                            CategoryChild.TaskList -> TaskListScreen(
                                sharedElementKeys = sharedElementKeys,
                                category = category,
                                controller = component.model,
                                onBack = component.navigation::navigateBack,
                                onReview = component.navigation::openReview,
                                onTask = { taskId ->
                                    component.navigation.openTask(taskId)
                                },
                            )

                            is CategoryChild.Review -> ReviewScreen(
                                categoryTitle = category.title,
                                controller = child.component.model,
                                onBack = component.navigation::navigateBack,
                            )

                            is CategoryChild.TaskDetails -> TaskDetailsStackHost(
                                sharedElementKey = sharedElementKeys.categoryTask(
                                    category.id,
                                    child.component.model.taskId,
                                ),
                                sharedElementKeys = sharedElementKeys,
                                component = child.component,
                                controller = child.component.model,
                            )
                        }
                    }
                }
            }
        }
    }
}
