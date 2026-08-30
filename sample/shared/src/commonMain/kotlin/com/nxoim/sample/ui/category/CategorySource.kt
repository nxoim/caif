package com.nxoim.sample.ui.category

import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import kotlinx.coroutines.flow.Flow

/** Data and mutations owned by the category task-list feature. */
interface CategorySource {
    fun getCategory(categoryId: String): Flow<KanbanCategory?>
    fun getCachedCategory(categoryId: String): KanbanCategory? = null
    fun getCachedTasks(categoryId: String, limit: Int = 10): List<KanbanTask> = emptyList()
    fun getActiveTaskPage(
        categoryId: String,
        startIndex: Int,
        pageSize: Int,
    ): Flow<List<KanbanTask>>

    fun archiveTask(taskId: String): Boolean
    fun deleteTask(taskId: String): Boolean
}
