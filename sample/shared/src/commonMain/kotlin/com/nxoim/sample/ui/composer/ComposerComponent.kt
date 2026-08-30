package com.nxoim.sample.ui.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.model.KanbanCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable

internal class TaskComposerComponent(
    private val context: ComponentContext,
    private val source: TaskComposerSource,
    private val categorySelectionSource: CategorySelectionSource,
    navigateToParent: () -> Unit,
) {
    internal val backHandler get() = context.backHandler
    private val navigationSource = ComposerNavigationImpl(
        navigateToParent = navigateToParent,
    )
    internal val navigation: ComposerNavigation = navigationSource

    val model = TaskComposerModel(source, navigation)

    val stack = context.childStack(
        source = navigationSource,
        serializer = ComposerDestination.serializer(),
        initialConfiguration = ComposerDestination.Writing,
        key = "TaskComposerStack",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(
        destination: ComposerDestination,
        childContext: ComponentContext,
    ): ComposerChild = when (destination) {
        ComposerDestination.Writing -> ComposerChild.Writing
        is ComposerDestination.CategorySelection -> ComposerChild.CategorySelection(
            CategorySelectionComponent(
                context = childContext,
                source = categorySelectionSource,
                taskTitle = destination.taskTitle,
                onCategorySelected = { categoryId ->
                    model.complete(
                        categoryId = categoryId,
                        taskTitle = destination.taskTitle,
                    )
                },
            )
        )
    }
}

internal interface ComposerNavigation {
    fun showCategorySelection(taskTitle: String)
    fun complete()
    fun navigateBack()
}

private class ComposerNavigationImpl(
    private val navigateToParent: () -> Unit,
) : ComposerNavigation, StackNavigation<ComposerDestination> by StackNavigation() {
    override fun showCategorySelection(taskTitle: String) {
        pushNew(ComposerDestination.CategorySelection(taskTitle))
    }

    override fun complete() = navigateToParent()

    override fun navigateBack() {
        pop { popped -> if (!popped) navigateToParent() }
    }
}

internal class TaskComposerModel(
    private val source: TaskComposerSource,
    val navigation: ComposerNavigation,
) : TaskComposerController {
    override var draftTitle by mutableStateOf("")
        private set

    override fun updateDraftTitle(title: String) {
        draftTitle = title
    }

    override fun submit() {
        val title = draftTitle.trim()
        if (title.isNotEmpty()) navigation.showCategorySelection(title)
    }

    fun complete(categoryId: String, taskTitle: String) {
        val title = taskTitle.trim()
        if (title.isNotEmpty() && source.addTask(categoryId, title) != null) {
            navigation.complete()
        }
    }
}

internal class CategorySelectionComponent(
    context: ComponentContext,
    source: CategorySelectionSource,
    taskTitle: String,
    onCategorySelected: (String) -> Unit,
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val model = CategorySelectionModel(source, taskTitle, onCategorySelected, modelScope)

    init {
        context.lifecycle.doOnDestroy(modelScope::cancel)
    }
}

internal class CategorySelectionModel(
    private val source: CategorySelectionSource,
    override val taskTitle: String,
    private val onCategorySelected: (String) -> Unit,
    modelScope: CoroutineScope,
) : CategorySelectionController {
    private val categoryPageSize = 2

    @OptIn(InternalPageableApi::class)
    override val categories: Pageable<Int, KanbanCategory> = pageable(
        coroutineScope = modelScope,
        onPage = { page ->
            val start = page * categoryPageSize
            source.getCategoryPage(
                startIndex = start,
                pageSize = categoryPageSize,
            )
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = categoryPageSize,
        ),
        initialItems = emptyList(),
        pageItemKey = KanbanCategory::id,
    )

    override fun selectCategory(categoryId: String) = onCategorySelected(categoryId)
}

internal sealed interface ComposerChild {
    data object Writing : ComposerChild
    data class CategorySelection(val component: CategorySelectionComponent) : ComposerChild
}

@Serializable
internal sealed interface ComposerDestination {
    @Serializable
    data object Writing : ComposerDestination

    @Serializable
    data class CategorySelection(val taskTitle: String) : ComposerDestination
}
