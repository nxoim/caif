package com.nxoim.sample.ui

import com.nxoim.sample.ui.board.BoardSource
import com.nxoim.sample.ui.category.CategorySource
import com.nxoim.sample.ui.composer.CategorySelectionSource
import com.nxoim.sample.ui.composer.TaskComposerSource
import com.nxoim.sample.ui.review.ReviewSource
import com.nxoim.sample.ui.task.NotesSource
import com.nxoim.sample.ui.task.TaskDetailsSource

/** Data contracts supplied by the composition root to the flow. */
interface FlowDependencies {
    val boardSource: BoardSource
    val categorySource: CategorySource
    val reviewSource: ReviewSource
    val taskDetailsSource: TaskDetailsSource
    val notesSource: NotesSource
    val taskComposerSource: TaskComposerSource
    val categorySelectionSource: CategorySelectionSource
}
