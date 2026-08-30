package com.nxoim.sample.ui.review

import androidx.collection.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxoim.evolpagink.compose.PageableComposeState
import com.nxoim.evolpagink.compose.toState
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.EmptyState
import com.nxoim.sample.ui.common.LoadingState
import com.nxoim.sample.ui.review.components.CardContext
import com.nxoim.sample.ui.review.components.PageableVisibilityEventsEffect
import com.nxoim.sample.ui.review.components.ReviewActions
import com.nxoim.sample.ui.review.components.ReviewAppBar
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun ReviewScreen(
    categoryTitle: String,
    controller: ReviewController,
    onBack: () -> Unit,
) {
    val pageableState = controller.remainingTasks.toState(
        state = rememberLazyListState(),
        key = KanbanTask::id,
    )
    PageableVisibilityEventsEffect(
        pageableState = pageableState,
        pageable = controller.remainingTasks,
    )

    val positions = remember {
        LruCache<String, CardContext.Position>(2)
    }

    fun decide(taskId: String, isDone: Boolean): Boolean {
        val position = if (isDone) CardContext.Position.Accepted else CardContext.Position.Declined
        positions.put(taskId, position)
        val success = controller.decide(taskId, isDone)
        if (!success) positions.remove(taskId)
        return success
    }

    Scaffold(
        topBar = {
            ReviewTopBar(
                categoryTitle = categoryTitle,
                onBack = onBack,
                controller = controller,
            )
        },
        bottomBar = {
            ReviewActions(
                enabled = pageableState.items.value.isNotEmpty(),
                onNotDone = {
                    pageableState.items.value.firstOrNull()?.let { task ->
                        decide(task.id, isDone = false)
                    }
                },
                onDone = {
                    pageableState.items.value.firstOrNull()?.let { task ->
                        decide(task.id, isDone = true)
                    }
                },
            )
        },
    ) { innerPadding ->
        ReviewContent(
            controller = controller,
            pageableState = pageableState,
            positions = positions,
            onDecision = ::decide,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun ReviewTopBar(
    categoryTitle: String,
    controller: ReviewController,
    onBack: () -> Unit,
) {
    val reviewState by controller.reviewState.collectAsState()

    ReviewAppBar(
        categoryTitle = categoryTitle,
        onBack = onBack,
        reviewState = reviewState,
    )
}

@Composable
private fun ReviewContent(
    controller: ReviewController,
    pageableState: PageableComposeState<KanbanTask>,
    positions: LruCache<String, CardContext.Position>,
    onDecision: (taskId: String, isDone: Boolean) -> Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val reviewState by controller.reviewState.collectAsState()
    val isFetchingPrevious by controller.remainingTasks.isFetchingPrevious.collectAsState()
    val isFetchingNext by controller.remainingTasks.isFetchingNext.collectAsState()
    val hasVisibleCards = pageableState.items.value.isNotEmpty()
    val shouldShowEmpty = !reviewState.isLoading &&
        !isFetchingPrevious &&
        !isFetchingNext &&
        !hasVisibleCards

    Box(
        modifier
            .padding(top = contentPadding.calculateTopPadding())
            .fillMaxSize()
    ) {
        ReviewCardStack(
            pageableState = pageableState,
            positions = positions,
            onDecision = onDecision,
            emptyContent = {
                if (shouldShowEmpty) EmptyState(
                    title = "All caught up",
                    description = "Every open task in this category has been reviewed.",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        )

        if (reviewState.isLoading) LoadingState()
    }
}

internal interface ReviewController {
    val reviewState: StateFlow<ReviewState>
    val remainingTasks: Pageable<Int, KanbanTask>

    fun decide(taskId: String, isDone: Boolean): Boolean
}
