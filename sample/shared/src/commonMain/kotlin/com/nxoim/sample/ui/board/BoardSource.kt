package com.nxoim.sample.ui.board

import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import kotlinx.coroutines.flow.Flow

/** Data the board feature needs to render and reset its categories. */
interface BoardSource {
    fun getCategoryPage(startIndex: Int, pageSize: Int): Flow<List<KanbanCategory>>
    fun getCategory(categoryId: String): Flow<KanbanCategory?>
    fun getActiveTaskPage(
        categoryId: String,
        startIndex: Int,
        pageSize: Int,
    ): Flow<List<KanbanTask>>

    fun reset()
}
