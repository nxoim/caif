package com.nxoim.sample.ui.board

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.board.components.CategoryCard
import com.nxoim.sample.ui.common.KanbanDefaults
import com.nxoim.sample.ui.common.ListItemShape
import com.nxoim.sample.ui.common.SharedElementKeyFactory
import com.nxoim.sample.ui.common.TaskRow
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds
import kotlinx.coroutines.flow.StateFlow

internal interface BoardController {
    val categories: Pageable<Int, BoardCategoryController>

    fun reset()
}

internal interface BoardCategoryController {
    val id: String
    val state: StateFlow<BoardCategoryState>
    val tasks: Pageable<Int, KanbanTask>
}

@Composable
internal fun BoardScreen(
    sharedElementKeys: SharedElementKeyFactory,
    controller: BoardController,
    onCategory: (String) -> Unit,
    onTask: (String) -> Unit,
    onComposer: () -> Unit,
) {
    val listState = rememberLazyListState()
    val categories = controller.categories.toState(listState)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Flow") },
                actions = {
                    IconButton(onClick = controller::reset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset board")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onComposer,
                modifier = Modifier.sharedBounds(
                    key = sharedElementKeys.composer(),
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
    ) { innerPadding ->
        LazyRow(
            state = listState,
            contentPadding = innerPadding + PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = categories.items.value,
                key = categories::key,
            ) { categoryController ->
                val categoryState by categoryController.state.collectAsState()

                categoryState.category?.let { category ->
                    CategoryCard(
                        category = category,
                        openTaskCount = categoryState.openTaskCount,
                        doneTaskCount = categoryState.doneTaskCount,
                        onCategory = { onCategory(category.id) },
                        modifier = Modifier
                            .width(320.dp)
                            .sharedBounds(
                                key = sharedElementKeys.category(category.id),
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            ),
                        content = {
                            val taskListState = rememberLazyListState()
                            val tasks = categoryController.tasks.toState(taskListState)
                            if (tasks.items.value.isEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(
                                            start = KanbanDefaults.cardPadding,
                                            end = KanbanDefaults.cardPadding,
                                            bottom = KanbanDefaults.cardPadding,
                                        ),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = KanbanDefaults.nestedCardShape,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "Nothing here yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = taskListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(
                                        start = KanbanDefaults.cardPadding,
                                        end = KanbanDefaults.cardPadding,
                                        top = 4.dp,
                                        bottom = KanbanDefaults.cardPadding,
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(
                                        KanbanDefaults.listItemSpacing,
                                    ),
                                ) {
                                    itemsIndexed(tasks) { index, task ->
                                        TaskRow(
                                            task = task,
                                            shape = ListItemShape.auto(index, tasks.items.value.size),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .sharedBounds(
                                                    key = sharedElementKeys.boardTask(task.id),
                                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                ),
                                            onClick = { onTask(task.id) },
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
