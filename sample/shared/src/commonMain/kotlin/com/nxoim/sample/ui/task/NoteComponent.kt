package com.nxoim.sample.ui.task

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.ui.common.LoadState
import com.nxoim.sample.ui.common.asLoadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class NoteComponent(
    private val context: ComponentContext,
    source: NotesSource,
    taskId: String,
    noteId: String,
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val model = NoteModel(source, taskId, noteId, modelScope)

    init {
        context.lifecycle.doOnDestroy(modelScope::cancel)
    }
}

internal class NoteModel(
    private val source: NotesSource,
    private val taskId: String,
    private val noteId: String,
    modelScope: CoroutineScope,
) : NoteController {
    override val note: StateFlow<LoadState<KanbanNote>> = source
        .getNote(taskId, noteId)
        .asLoadState()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = LoadState.Loading,
        )

    override fun updateTitle(title: String): Boolean = source.updateNoteTitle(taskId, noteId, title)
    override fun updateText(text: String): Boolean = source.updateNote(taskId, noteId, text)
}
