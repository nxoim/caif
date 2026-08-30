package com.nxoim.sample.ui

import com.nxoim.sample.data.FakeKanbanSource
import com.nxoim.sample.ui.board.BoardSource
import com.nxoim.sample.ui.category.CategorySource
import com.nxoim.sample.ui.composer.CategorySelectionSource
import com.nxoim.sample.ui.composer.TaskComposerSource
import com.nxoim.sample.ui.review.ReviewSource
import com.nxoim.sample.ui.task.NotesSource
import com.nxoim.sample.ui.task.TaskDetailsSource

/** Composition-root wiring for the in-memory sample data implementation. */
class FakeFlowDependencies : FlowDependencies {
    private val source = FakeKanbanSource()

    override val boardSource: BoardSource get() = source
    override val categorySource: CategorySource get() = source
    override val reviewSource: ReviewSource get() = source
    override val taskDetailsSource: TaskDetailsSource get() = source
    override val notesSource: NotesSource get() = source
    override val taskComposerSource: TaskComposerSource get() = source
    override val categorySelectionSource: CategorySelectionSource get() = source
}
