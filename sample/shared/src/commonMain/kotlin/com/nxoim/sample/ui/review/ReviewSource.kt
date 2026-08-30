package com.nxoim.sample.ui.review

import com.nxoim.sample.model.KanbanTask
import kotlinx.coroutines.flow.Flow

/** The smallest data contract needed by review. */
interface ReviewSource {
    fun getTasks(categoryId: String): Flow<List<KanbanTask>>

    fun completeTask(taskId: String): Boolean
}
