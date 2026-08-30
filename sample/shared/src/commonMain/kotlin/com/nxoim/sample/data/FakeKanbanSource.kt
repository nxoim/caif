package com.nxoim.sample.data

import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.model.TaskStatus
import com.nxoim.sample.ui.board.BoardSource
import com.nxoim.sample.ui.category.CategorySource
import com.nxoim.sample.ui.composer.CategorySelectionSource
import com.nxoim.sample.ui.composer.TaskComposerSource
import com.nxoim.sample.ui.review.ReviewSource
import com.nxoim.sample.ui.task.NotesSource
import com.nxoim.sample.ui.task.TaskDetailsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory sample data */
internal class FakeKanbanSource :
    BoardSource,
    CategorySource,
    ReviewSource,
    TaskDetailsSource,
    NotesSource,
    TaskComposerSource,
    CategorySelectionSource {
    private val board = MutableStateFlow(seedBoard())

    override fun getCategoryPage(startIndex: Int, pageSize: Int): Flow<List<KanbanCategory>> =
        board.map { it.categories.map(::categoryModel).page(startIndex, pageSize) }

    override fun getCategory(categoryId: String): Flow<KanbanCategory?> =
        board.map { state ->
            state.categories.firstOrNull { it.id == categoryId }?.let(::categoryModel)
        }

    override fun getCachedCategory(categoryId: String): KanbanCategory? =
        board.value.categories.firstOrNull { it.id == categoryId }?.let(::categoryModel)

    override fun getCachedTasks(categoryId: String, limit: Int): List<KanbanTask> =
        board.value.categories.firstOrNull { it.id == categoryId }
            ?.tasks.orEmpty()
            .filter { it.status != TaskStatus.Archived }
            .take(limit)
            .map(::taskModel)

    override fun getActiveTaskPage(
        categoryId: String,
        startIndex: Int,
        pageSize: Int,
    ): Flow<List<KanbanTask>> = getTasks(categoryId).map { tasks ->
        tasks.filter { it.status != TaskStatus.Archived }.page(startIndex, pageSize)
    }

    override fun getTasks(categoryId: String): Flow<List<KanbanTask>> =
        board.map { state ->
            state.categories.firstOrNull { it.id == categoryId }?.tasks.orEmpty().map(::taskModel)
        }

    override fun getTask(taskId: String): Flow<KanbanTask?> =
        board.map { state -> state.findTask(taskId)?.let(::taskModel) }

    override fun getCachedTask(taskId: String): KanbanTask? =
        board.value.findTask(taskId)?.let(::taskModel)

    override fun getNotePage(
        taskId: String,
        startIndex: Int,
        pageSize: Int
    ): Flow<List<KanbanNote>> =
        board.map { state -> state.notesFor(taskId).map(::noteModel).page(startIndex, pageSize) }

    override fun getNote(taskId: String, noteId: String): Flow<KanbanNote?> =
        board.map { state ->
            state.notesFor(taskId).firstOrNull { it.id == noteId }?.let(::noteModel)
        }

    override fun addTask(categoryId: String, title: String): KanbanTask? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null

        var createdTask: KanbanTask? = null
        board.update { state ->
            if (state.categories.none { it.id == categoryId }) {
                createdTask = null
                return@update state
            }

            val taskRecord = TaskRecord(
                id = "task-${state.nextTaskNumber}",
                title = trimmed,
                description = "Captured from the board.",
            )
            createdTask = taskModel(taskRecord)

            state.copy(
                categories = state.categories.map { category ->
                    if (category.id == categoryId) category.copy(tasks = category.tasks + taskRecord) else category
                },
                nextTaskNumber = state.nextTaskNumber + 1,
            )
        }
        return createdTask
    }

    override fun completeTask(taskId: String): Boolean =
        updateTask(taskId) { it.copy(status = TaskStatus.Done) }

    override fun archiveTask(taskId: String): Boolean =
        updateTask(taskId) { it.copy(status = TaskStatus.Archived) }

    override fun deleteTask(taskId: String): Boolean {
        var didDelete = false
        board.update { state ->
            if (state.findTask(taskId) == null) {
                didDelete = false
                state
            } else {
                didDelete = true
                state.copy(
                    categories = state.categories.map { category ->
                        category.copy(tasks = category.tasks.filterNot { it.id == taskId })
                    }
                )
            }
        }
        return didDelete
    }

    override fun updateNote(taskId: String, noteId: String, text: String): Boolean {
        var didUpdate = false
        board.update { state ->
            if (state.notesFor(taskId).none { it.id == noteId }) {
                didUpdate = false
                state
            } else {
                didUpdate = true
                state.copy(
                    notes = state.notes.map { note ->
                        if (note.id == noteId && note.taskId == taskId) note.copy(text = text) else note
                    }
                )
            }
        }
        return didUpdate
    }

    override fun updateNoteTitle(taskId: String, noteId: String, title: String): Boolean {
        var didUpdate = false
        board.update { state ->
            if (state.notesFor(taskId).none { it.id == noteId }) {
                didUpdate = false
                state
            } else {
                didUpdate = true
                state.copy(
                    notes = state.notes.map { note ->
                        if (note.id == noteId && note.taskId == taskId) note.copy(title = title) else note
                    }
                )
            }
        }
        return didUpdate
    }

    override fun reset() {
        board.value = seedBoard()
    }

    private fun updateTask(taskId: String, transform: (TaskRecord) -> TaskRecord): Boolean {
        var didUpdate = false
        board.update { state ->
            if (state.findTask(taskId) == null) {
                didUpdate = false
                state
            } else {
                didUpdate = true
                state.copy(
                    categories = state.categories.map { category ->
                        category.copy(
                            tasks = category.tasks.map { task ->
                                if (task.id == taskId) transform(task) else task
                            }
                        )
                    }
                )
            }
        }
        return didUpdate
    }
}
