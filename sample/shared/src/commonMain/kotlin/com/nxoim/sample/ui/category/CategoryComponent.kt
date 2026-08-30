package com.nxoim.sample.ui.category

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.LoadState
import com.nxoim.sample.ui.common.asLoadState
import com.nxoim.sample.ui.review.ReviewComponent
import com.nxoim.sample.ui.review.ReviewSource
import com.nxoim.sample.ui.task.NotesSource
import com.nxoim.sample.ui.task.TaskDetailsComponent
import com.nxoim.sample.ui.task.TaskDetailsSource
import com.nxoim.sample.ui.tasks.TaskListController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

internal class CategoryComponent(
    private val context: ComponentContext,
    private val source: CategorySource,
    private val reviewSource: ReviewSource,
    private val taskDetailsSource: TaskDetailsSource,
    private val notesSource: NotesSource,
    val categoryId: String,
    navigateToParent: () -> Unit,
) {
    internal val backHandler get() = context.backHandler
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val navigationSource = CategoryNavigationImpl(navigateToParent)
    internal val navigation: CategoryNavigation = navigationSource
    val model = CategoryModel(source, categoryId, modelScope)

    init {
        context.lifecycle.doOnDestroy(modelScope::cancel)
    }

    val stack = context.childStack(
        source = navigationSource,
        serializer = CategoryDestination.serializer(),
        initialConfiguration = CategoryDestination.TaskList,
        key = "CategoryStack-$categoryId",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(
        destination: CategoryDestination,
        childContext: ComponentContext,
    ): CategoryChild = when (destination) {
        CategoryDestination.TaskList -> CategoryChild.TaskList
        CategoryDestination.Review -> CategoryChild.Review(
            ReviewComponent(
                context = childContext,
                source = reviewSource,
                categoryId = categoryId,
            ),
        )

        is CategoryDestination.TaskDetails -> CategoryChild.TaskDetails(
            TaskDetailsComponent(
                context = childContext,
                source = taskDetailsSource,
                notesSource = notesSource,
                taskId = destination.taskId,
                navigateToParent = navigation::navigateBack,
            )
        )
    }
}

internal class CategoryModel(
    private val source: CategorySource,
    val categoryId: String,
    modelScope: CoroutineScope,
) : TaskListController {
    private val taskPageSize = 10
    private val cachedCategory = source.getCachedCategory(categoryId)

    val category: StateFlow<LoadState<KanbanCategory>> = source
        .getCategory(categoryId)
        .asLoadState()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = cachedCategory
                ?.let { LoadState.Content(it) }
                ?: LoadState.Loading,
        )

    @OptIn(InternalPageableApi::class)
    override val tasks = pageable(
        coroutineScope = modelScope,
        onPage = { page ->
            val start = page * taskPageSize
            source.getActiveTaskPage(
                categoryId = categoryId,
                startIndex = start,
                pageSize = taskPageSize,
            )
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = 4,
        ),
        initialItems = source.getCachedTasks(categoryId, limit = taskPageSize),
        pageItemKey = KanbanTask::id,
    )

    override fun archiveTask(taskId: String): Boolean = source.archiveTask(taskId)
    override fun deleteTask(taskId: String): Boolean = source.deleteTask(taskId)
}

internal interface CategoryNavigation {
    fun openReview()
    fun openTask(taskId: String)
    fun navigateBack()
}

private class CategoryNavigationImpl(
    private val navigateToParent: () -> Unit,
) : CategoryNavigation, StackNavigation<CategoryDestination> by StackNavigation() {
    override fun openReview() {
        pushNew(CategoryDestination.Review)
    }

    override fun openTask(taskId: String) {
        pushNew(CategoryDestination.TaskDetails(taskId))
    }

    override fun navigateBack() {
        pop { popped -> if (!popped) navigateToParent() }
    }
}

internal sealed interface CategoryChild {
    data object TaskList : CategoryChild
    data class Review(val component: ReviewComponent) : CategoryChild
    data class TaskDetails(val component: TaskDetailsComponent) : CategoryChild
}

@Serializable
internal sealed interface CategoryDestination {
    @Serializable
    data object TaskList : CategoryDestination

    @Serializable
    data object Review : CategoryDestination

    @Serializable
    data class TaskDetails(val taskId: String) : CategoryDestination
}
