package com.nxoim.sample.ui.board

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.evolpagink.core.InternalPageableApi
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class BoardComponent(
    private val context: ComponentContext,
    source: BoardSource,
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val model = BoardModel(source, modelScope)

    init {
        context.lifecycle.doOnDestroy {
            model.close()
            modelScope.cancel()
        }
    }
}

internal class BoardModel(
    private val source: BoardSource,
    private val modelScope: CoroutineScope,
) : BoardController {
    private val categoryPageSize = 2
    private val categoryModels = BoardCategoryModelCache(source, modelScope)

    override val categories: Pageable<Int, BoardCategoryController> = pageable(
        coroutineScope = modelScope,
        onPage = { page ->
            val start = page * categoryPageSize
            source
                .getCategoryPage(
                    startIndex = start,
                    pageSize = categoryPageSize,
                )
                .map { categories -> categories.map(categoryModels::getOrCreate) }
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = categoryPageSize,
        ),
        initialItems = emptyList(),
        pageItemKey = BoardCategoryController::id,
    )

    init {
        modelScope.launch {
            var hasLoadedCategories = categories.items.value.isNotEmpty()
            categories.items.collect { loadedCategories ->
                if (!hasLoadedCategories && loadedCategories.isEmpty()) return@collect

                hasLoadedCategories = true
                categoryModels.retain(loadedCategories.mapTo(mutableSetOf()) { it.id })
            }
        }
    }

    override fun reset() = source.reset()

    fun close() {
        categoryModels.clear()
    }
}

@OptIn(InternalPageableApi::class)
internal class BoardCategoryModel(
    private val source: BoardSource,
    override val id: String,
    initialCategory: KanbanCategory,
    parentScope: CoroutineScope,
) : BoardCategoryController {
    private val scope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]),
    )

    override val state: StateFlow<BoardCategoryState> = source
        .getCategory(id)
        .map(::stateFor)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = stateFor(initialCategory),
        )

    private val taskPageSize = 2

    override val tasks: Pageable<Int, KanbanTask> = pageable(
        coroutineScope = scope,
        onPage = { page ->
            val start = page * taskPageSize
            source.getActiveTaskPage(
                categoryId = id,
                startIndex = start,
                pageSize = taskPageSize,
            )
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = taskPageSize,
        ),
        initialItems = initialCategory.tasks
            .filter { it.status != TaskStatus.Archived }
            .take(taskPageSize),
        pageItemKey = KanbanTask::id,
    )

    private fun stateFor(category: KanbanCategory?): BoardCategoryState {
        return BoardCategoryState(
            category = category,
            openTaskCount = category?.tasks.orEmpty().count { it.status == TaskStatus.Open },
            doneTaskCount = category?.tasks.orEmpty().count { it.status == TaskStatus.Done },
        )
    }

    fun close() {
        scope.cancel()
    }
}

internal data class BoardCategoryState(
    val category: KanbanCategory?,
    val openTaskCount: Int,
    val doneTaskCount: Int,
)

private class BoardCategoryModelCache(
    private val source: BoardSource,
    private val parentScope: CoroutineScope,
) {
    private val instances = mutableMapOf<String, BoardCategoryModel>()

    fun getOrCreate(category: KanbanCategory): BoardCategoryController =
        instances.getOrPut(category.id) {
            BoardCategoryModel(
                source = source,
                id = category.id,
                initialCategory = category,
                parentScope = parentScope,
            )
        }

    fun retain(loadedIds: Set<String>) {
        instances.keys
            .filterNot(loadedIds::contains)
            .toList()
            .forEach { id ->
                instances.remove(id)?.close()
            }
    }

    fun clear() {
        instances.values.forEach(BoardCategoryModel::close)
        instances.clear()
    }
}