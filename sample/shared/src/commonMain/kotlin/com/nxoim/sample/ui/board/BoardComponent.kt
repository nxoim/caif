package com.nxoim.sample.ui.board

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
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
        context.lifecycle.doOnDestroy(modelScope::cancel)
    }
}

internal class BoardModel(
    private val source: BoardSource,
    private val modelScope: CoroutineScope,
) : BoardController {
    private val categoryPageSize = 2
    private val categoryModels = BoardCategoryModelCache(source, modelScope)

    override val categories = pageable(
        coroutineScope = modelScope,
        onPage = { page ->
            val start = page * categoryPageSize
            source
                .getCategoryPage(
                    startIndex = start,
                    pageSize = categoryPageSize,
                )
                .map { categories -> categories.fastMap(categoryModels::getOrCreate) }
        },
        strategy = prefetchMinimumItemAmount(
            minimumItemAmount = categoryPageSize,
        ),
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
}

internal class BoardCategoryModel(
    private val source: BoardSource,
    override val id: String,
    initialCategory: KanbanCategory,
    private val scope: CoroutineScope,
) : BoardCategoryController {
    override val state = source
        .getCategory(id)
        .map(::stateFor)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = stateFor(initialCategory),
        )

    private val taskPageSize = 5

    override val tasks = pageable(
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
            .fastFilter { it.status != TaskStatus.Archived }
            .take(taskPageSize),
        pageItemKey = KanbanTask::id,
    )

    private fun stateFor(category: KanbanCategory?) = BoardCategoryState(
        category = category,
        openTaskCount = category?.tasks.orEmpty().count { it.status == TaskStatus.Open },
        doneTaskCount = category?.tasks.orEmpty().count { it.status == TaskStatus.Done },
    )
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
                scope = parentScope,
            )
        }

    fun retain(loadedIds: Set<String>) {
        instances.keys
            .filterNot(loadedIds::contains)
            .fastForEach(instances::remove)
    }
}
