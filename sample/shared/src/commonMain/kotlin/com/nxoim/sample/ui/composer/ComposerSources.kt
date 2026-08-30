package com.nxoim.sample.ui.composer

import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import kotlinx.coroutines.flow.Flow

interface TaskComposerSource {
    fun addTask(categoryId: String, title: String): KanbanTask?
}

interface CategorySelectionSource {
    fun getCategoryPage(startIndex: Int, pageSize: Int): Flow<List<KanbanCategory>>
}
