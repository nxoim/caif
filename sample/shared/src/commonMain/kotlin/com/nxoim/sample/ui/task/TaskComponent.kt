package com.nxoim.sample.ui.task

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.LoadState
import com.nxoim.sample.ui.common.asLoadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

internal class TaskDetailsComponent(
    private val context: ComponentContext,
    private val source: TaskDetailsSource,
    private val notesSource: NotesSource,
    taskId: String,
    navigateToParent: () -> Unit,
) {
    internal val backHandler get() = context.backHandler
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val navigationSource = TaskNavigationImpl(navigateToParent)
    internal val navigation: TaskNavigation = navigationSource
    val model = TaskDetailsModel(source, notesSource, taskId, modelScope)

    init {
        context.lifecycle.doOnDestroy(modelScope::cancel)
    }

    val stack = context.childStack(
        source = navigationSource,
        serializer = TaskDestination.serializer(),
        initialConfiguration = TaskDestination.Details,
        key = "TaskStack-$taskId",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(
        destination: TaskDestination,
        childContext: ComponentContext,
    ): TaskChild = when (destination) {
        TaskDestination.Details -> TaskChild.Details
        is TaskDestination.Note -> TaskChild.Note(
            NoteComponent(childContext, notesSource, model.taskId, destination.noteId),
        )
    }
}

internal class TaskDetailsModel(
    private val source: TaskDetailsSource,
    private val notesSource: NotesSource,
    val taskId: String,
    modelScope: CoroutineScope,
) : TaskDetailsController {
    private val notePageSize = 5
    private val cachedTask = source.getCachedTask(taskId)

    override val task: StateFlow<LoadState<KanbanTask>> = source
        .getTask(taskId)
        .asLoadState()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = cachedTask?.let { LoadState.Content(it) } ?: LoadState.Loading,
        )

    @OptIn(InternalPageableApi::class)
    override val notes: Pageable<Int, KanbanNote> = pageable(
        coroutineScope = modelScope,
        onPage = { page ->
            val start = page * notePageSize
            notesSource.getNotePage(
                taskId = taskId,
                startIndex = start,
                pageSize = notePageSize,
            )
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = notePageSize,
        ),
        initialItems = emptyList(),
        pageItemKey = KanbanNote::id,
    )
}

internal interface TaskNavigation {
    fun openNote(noteId: String)
    fun navigateBack()
}

private class TaskNavigationImpl(
    private val navigateToParent: () -> Unit,
) : TaskNavigation, StackNavigation<TaskDestination> by StackNavigation() {
    override fun openNote(noteId: String) = pushNew(TaskDestination.Note(noteId))

    override fun navigateBack() {
        pop { popped ->
            if (!popped) navigateToParent()
        }
    }
}

internal sealed interface TaskChild {
    data object Details : TaskChild
    data class Note(val component: NoteComponent) : TaskChild
}

@Serializable
internal sealed interface TaskDestination {
    @Serializable
    data object Details : TaskDestination

    @Serializable
    data class Note(val noteId: String) : TaskDestination
}
