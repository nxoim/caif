package com.nxoim.sample.ui

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import kotlinx.serialization.Serializable

internal interface FlowNavigation {
    fun openCategory(categoryId: String)
    fun openTask(taskId: String)
    fun openTaskComposer()
    fun navigateBack()
}

internal class FlowNavigationImpl(
    private val onExit: () -> Unit,
) : FlowNavigation, StackNavigation<FlowDestination> by StackNavigation() {
    override fun openCategory(categoryId: String) {
        pushNew(FlowDestination.Category(categoryId))
    }

    override fun openTask(taskId: String) {
        pushNew(FlowDestination.TaskDetails(taskId))
    }

    override fun openTaskComposer() {
        pushNew(FlowDestination.TaskComposer)
    }

    override fun navigateBack() {
        pop { popped -> if (!popped) onExit() }
    }
}

@Serializable
internal sealed interface FlowDestination {
    @Serializable
    data object Board : FlowDestination

    @Serializable
    data class Category(val categoryId: String) : FlowDestination

    @Serializable
    data class TaskDetails(val taskId: String) : FlowDestination

    @Serializable
    data object TaskComposer : FlowDestination
}
