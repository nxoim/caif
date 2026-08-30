package com.nxoim.sample.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.childStack
import com.nxoim.sample.ui.board.BoardComponent
import com.nxoim.sample.ui.category.CategoryComponent
import com.nxoim.sample.ui.composer.TaskComposerComponent
import com.nxoim.sample.ui.task.TaskDetailsComponent

class FlowComponent(
    private val context: ComponentContext,
    private val dependencies: FlowDependencies,
    onExit: () -> Unit = {},
) {
    private val navigationSource = FlowNavigationImpl(onExit)
    internal val navigation: FlowNavigation = navigationSource
    internal val backHandler get() = context.backHandler

    internal val stack = context.childStack(
        source = navigationSource,
        serializer = FlowDestination.serializer(),
        initialConfiguration = FlowDestination.Board,
        key = "FlowRootStack",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(
        destination: FlowDestination,
        childContext: ComponentContext,
    ): FlowChild = when (destination) {
        FlowDestination.Board -> FlowChild.Board(
            BoardComponent(childContext, dependencies.boardSource),
        )

        is FlowDestination.Category -> FlowChild.Category(
            CategoryComponent(
                context = childContext,
                source = dependencies.categorySource,
                reviewSource = dependencies.reviewSource,
                taskDetailsSource = dependencies.taskDetailsSource,
                notesSource = dependencies.notesSource,
                categoryId = destination.categoryId,
                navigateToParent = navigation::navigateBack,
            )
        )

        is FlowDestination.TaskDetails -> FlowChild.TaskDetails(
            TaskDetailsComponent(
                context = childContext,
                source = dependencies.taskDetailsSource,
                notesSource = dependencies.notesSource,
                taskId = destination.taskId,
                navigateToParent = navigation::navigateBack,
            )
        )

        FlowDestination.TaskComposer -> FlowChild.TaskComposer(
            TaskComposerComponent(
                context = childContext,
                source = dependencies.taskComposerSource,
                categorySelectionSource = dependencies.categorySelectionSource,
                navigateToParent = navigation::navigateBack,
            )
        )
    }
}

internal sealed interface FlowChild {
    data class Board(val component: BoardComponent) : FlowChild
    data class Category(val component: CategoryComponent) : FlowChild
    data class TaskDetails(val component: TaskDetailsComponent) : FlowChild
    data class TaskComposer(val component: TaskComposerComponent) : FlowChild
}
