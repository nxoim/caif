package com.nxoim.sample.ui.tasks

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.caif.prefabs.list.rememberCollectionSwipeAnimator
import com.nxoim.caif.swipeable.SwipeConstraint
import com.nxoim.caif.swipeable.SwipeThresholds
import com.nxoim.caif.swipeable.swipeable
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.EmptyState
import com.nxoim.sample.ui.common.KanbanDefaults
import com.nxoim.sample.ui.common.ListItemShape
import com.nxoim.sample.ui.common.SharedElementKeyFactory
import com.nxoim.sample.ui.common.TaskRow
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds
import com.nxoim.sample.ui.tasks.components.CategoryHeader
import com.nxoim.sample.ui.tasks.components.MagneticListAnimationContext
import com.nxoim.sample.ui.tasks.components.TaskSwipeAction
import com.nxoim.sample.ui.tasks.components.TaskSwipeActions
import com.nxoim.sample.ui.tasks.components.buildMagneticListAnimation
import com.nxoim.sample.ui.tasks.components.dispatchSwipeEnd
import com.nxoim.sample.ui.tasks.components.dispatchSwipeUpdate
import com.nxoim.sample.ui.tasks.components.rememberMagneticAnimationEnvironmentState
import com.nxoim.sample.ui.tasks.components.rememberTaskSwipeActionsState

@Composable
internal fun TaskListScreen(
    sharedElementKeys: SharedElementKeyFactory,
    category: KanbanCategory,
    controller: TaskListController,
    onBack: () -> Unit,
    onReview: () -> Unit,
    onTask: (String) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val animationEnvironment by rememberMagneticAnimationEnvironmentState()
        val listState = rememberLazyListState()
        val pageableState = controller.tasks.toState(listState)
        val animator = rememberCollectionSwipeAnimator(
            settleableFactory = { buildMagneticListAnimation() },
            contextFor = { position ->
                MagneticListAnimationContext(
                    position = position,
                    environment = animationEnvironment,
                )
            },
            visibleKeys = {
                listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
            },
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Category") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                CategoryHeader(
                    category = category,
                    onReview = onReview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = innerPadding.calculateTopPadding() + 12.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 12.dp,
                        ),
                )

                if (pageableState.items.value.isEmpty()) {
                    EmptyState(
                        title = "Nothing to review",
                        description = "This category has no active tasks.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                    ) {
                        itemsIndexed(pageableState) { index, task ->
                            val swipeActionsState = rememberTaskSwipeActionsState(
                                onDismiss = { action ->
                                    when (action) {
                                        TaskSwipeAction.Archive -> controller.archiveTask(task.id)
                                        TaskSwipeAction.Delete -> controller.deleteTask(task.id)
                                    }
                                },
                            )

                            TaskSwipeActions(
                                state = swipeActionsState,
                                modifier = animator.modifierFor(task.id),
                            ) {
                                TaskRow(
                                    task = task,
                                    shape = ListItemShape.auto(index, pageableState.items.value.size),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 16.dp,
                                            vertical = KanbanDefaults.listItemSpacing,
                                        )
                                        .sharedBounds(
                                            key = sharedElementKeys.categoryTask(category.id, task.id),
                                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                        )
                                        .swipeable(
                                            detectionConstraint = SwipeConstraint.start(),
                                            thresholds = SwipeThresholds.Default.copy(
                                                confirmationMinDistance =
                                                    TaskSwipeDistanceConfirmationLimit,
                                            ),
                                            onStart = {
                                                animator.onStart(task.id)
                                            },
                                            onProgress = { delta, _, _ ->
                                                swipeActionsState.onSwipeDelta(delta.x)
                                                animator.dispatchSwipeUpdate(delta)
                                            },
                                            onConfirm = { velocity, _ ->
                                                animator.dispatchSwipeEnd(velocity)
                                                animator.onEnd()
                                                swipeActionsState.dismiss(TaskSwipeAction.Archive)
                                            },
                                            onCancel = { velocity ->
                                                swipeActionsState.onSwipeEnd(velocity)
                                                animator.dispatchSwipeEnd(velocity)
                                                animator.onEnd()
                                            },
                                        ),
                                    onClick = { onTask(task.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


internal interface TaskListController {
    val tasks: Pageable<Int, KanbanTask>

    fun archiveTask(taskId: String): Boolean
    fun deleteTask(taskId: String): Boolean
}

private val TaskSwipeDistanceConfirmationLimit = 1000.dp
