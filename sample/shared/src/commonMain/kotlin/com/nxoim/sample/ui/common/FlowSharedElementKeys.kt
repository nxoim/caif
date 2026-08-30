package com.nxoim.sample.ui.common

/** Navigation-layer identity for Flow's optional shared-element wiring. */
internal object FlowSharedElementKeys : SharedElementKeyFactory {
    override fun category(categoryId: String): Any = FlowSharedElementKey.Category(categoryId)

    override fun boardTask(taskId: String): Any = FlowSharedElementKey.BoardTask(taskId)

    override fun categoryTask(categoryId: String, taskId: String): Any =
        FlowSharedElementKey.CategoryTask(categoryId, taskId)

    override fun note(taskId: String, noteId: String): Any =
        FlowSharedElementKey.Note(taskId, noteId)

    override fun composer(): Any = FlowSharedElementKey.Composer
}

private sealed interface FlowSharedElementKey {
    data object Composer : FlowSharedElementKey
    data class Category(val id: String) : FlowSharedElementKey
    // contextual. we do not transition elements between
    // lists in the board screen and the task list screen
    data class BoardTask(val id: String) : FlowSharedElementKey
    data class CategoryTask(val categoryId: String, val taskId: String) : FlowSharedElementKey
    data class Note(val taskId: String, val noteId: String) : FlowSharedElementKey
}
