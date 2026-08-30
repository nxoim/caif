package com.nxoim.sample.ui.common

internal interface SharedElementKeyFactory {
    fun category(categoryId: String): Any
    fun note(taskId: String, noteId: String): Any
    fun composer(): Any
    fun boardTask(taskId: String): Any
    fun categoryTask(categoryId: String, taskId: String): Any
}
