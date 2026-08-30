package com.nxoim.sample.ui.task

import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.model.KanbanTask
import kotlinx.coroutines.flow.Flow

interface TaskDetailsSource {
    fun getTask(taskId: String): Flow<KanbanTask?>
    fun getCachedTask(taskId: String): KanbanTask? = null
}

interface NotesSource {
    fun getNotePage(taskId: String, startIndex: Int, pageSize: Int): Flow<List<KanbanNote>>
    fun getNote(taskId: String, noteId: String): Flow<KanbanNote?>
    fun updateNote(taskId: String, noteId: String, text: String): Boolean
    fun updateNoteTitle(taskId: String, noteId: String, title: String): Boolean
}
