package com.nxoim.sample.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nxoim.evolpagink.compose.toState
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.sample.model.KanbanCategory
import com.nxoim.sample.ui.common.KanbanDefaults
import com.nxoim.sample.ui.common.ListItemShape

internal interface TaskComposerController {
    val draftTitle: String

    fun updateDraftTitle(title: String)
    fun submit()
}

internal interface CategorySelectionController {
    val taskTitle: String
    val categories: Pageable<Int, KanbanCategory>

    fun selectCategory(categoryId: String)
}

@Composable
internal fun WritingScreen(
    controller: TaskComposerController,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Capture something for the board.",
                style = MaterialTheme.typography.titleLarge,
            )
            TextField(
                value = controller.draftTitle,
                onValueChange = controller::updateDraftTitle,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "What needs to be done?",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                minLines = 2,
                maxLines = 4,
            )
            Button(
                onClick = controller::submit,
                enabled = controller.draftTitle.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Done / Add")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun CategorySelectionScreen(
    controller: CategorySelectionController,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val categories = controller.categories.toState(listState)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Choose category") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(KanbanDefaults.listItemSpacing),
        ) {
            item {
                Text(
                    text = "Add “${controller.taskTitle}” to:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            itemsIndexed(
                items = categories.items.value,
                key = { _, category -> categories.key(category) },
            ) { index, category ->
                Card(
                    onClick = { controller.selectCategory(category.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ListItemShape.auto(index, categories.items.value.size),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            KanbanDefaults.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(category.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = category.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Select category",
                        )
                    }
                }
            }
        }
    }
}
