@file:OptIn(InternalPageableApi::class)

package com.nxoim.sample.ui.review

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class ReviewComponent(
    context: ComponentContext,
    source: ReviewSource,
    categoryId: String,
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val model = ReviewModel(
        source = source,
        categoryId = categoryId,
        coroutineScope = modelScope,
    )

    init {
        context.lifecycle.doOnDestroy(modelScope::cancel)
    }
}

internal data class ReviewState(
    val isLoading: Boolean,
    val total: Int,
    val reviewed: Int,
)

internal class ReviewModel(
    private val source: ReviewSource,
    private val categoryId: String,
    coroutineScope: CoroutineScope,
) : ReviewController {
    private val pageSize = 2
    private val reviewedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val categoryTasks = source
        .getTasks(categoryId)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    override val reviewState: StateFlow<ReviewState> = combine(
        categoryTasks,
        reviewedTaskIds,
    ) { tasks, reviewedIds ->
        reviewStateFor(tasks, reviewedIds)
    }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = reviewStateFor(tasks = null, reviewedTaskIds = emptySet()),
        )

    override val remainingTasks: Pageable<Int, KanbanTask> = pageable(
        coroutineScope = coroutineScope,
        context = reviewedTaskIds,
        onPage = { page ->
            val start = page * pageSize
            val reviewedIds = this
            categoryTasks.map { tasks ->
                tasks?.let {
                    reviewableTasks(it, reviewedIds)
                        .drop(start)
                        .take(pageSize)
                        .takeIf { pageItems -> pageItems.isNotEmpty() }
                }
            }
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = pageSize,
        ),
        pageItemKey = KanbanTask::id,
    )

    /**
     * Records one decision for the current review pass. An open task is intentionally kept open
     * when [isDone] is false, but it is excluded from this pass by [reviewedTaskIds].
     */
    override fun decide(taskId: String, isDone: Boolean): Boolean {
        val task = categoryTasks.value
            .orEmpty()
            .firstOrNull { task ->
                task.id == taskId && task.isReviewable(reviewedTaskIds.value)
            }
            ?: return false

        if (isDone && !source.completeTask(task.id)) return false

        reviewedTaskIds.update { it + task.id }
        return true
    }

    private fun reviewStateFor(
        tasks: List<KanbanTask>?,
        reviewedTaskIds: Set<String>,
    ): ReviewState {
        if (tasks == null) {
            return ReviewState(
                isLoading = true,
                total = 0,
                reviewed = reviewedTaskIds.size,
            )
        }

        return ReviewState(
            isLoading = false,
            total = reviewedTaskIds.size + tasks.count { task ->
                task.isReviewable(reviewedTaskIds)
            },
            reviewed = reviewedTaskIds.size,
        )
    }

    private companion object {
        fun reviewableTasks(
            tasks: List<KanbanTask>,
            reviewedTaskIds: Set<String>,
        ): List<KanbanTask> = tasks.filter { task -> task.isReviewable(reviewedTaskIds) }

        fun KanbanTask.isReviewable(reviewedTaskIds: Set<String>): Boolean =
            status == TaskStatus.Open && id !in reviewedTaskIds
    }
}
