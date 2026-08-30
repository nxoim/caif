package com.nxoim.sample.ui.task

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nxoim.evolpagink.compose.PageableComposeState
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.KanbanDefaults
import com.nxoim.sample.ui.common.ListItemShape
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds

@Composable
internal fun TaskDetailContent(
    task: KanbanTask,
    notes: PageableComposeState<KanbanNote>,
    noteListState: LazyListState,
    provideNoteSharedKey: ((KanbanNote) -> Any)? = null,
    onBack: () -> Unit,
    onNote: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = noteListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(KanbanDefaults.listItemSpacing),
        ) {
            item(key = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = task.status.name,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(task.title, style = MaterialTheme.typography.headlineSmall)
                    Text(task.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item(key = 1) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
            if (notes.items.value.isEmpty()) {
                item(key = 2) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = ListItemShape.Single,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No notes yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(notes) { index, note ->
                    NoteRow(
                        note = note,
                        shape = ListItemShape.auto(index, notes.items.value.size),
                        onClick = { onNote(note.id) },
                        modifier = Modifier.sharedBounds(
                            key = provideNoteSharedKey?.invoke(note),
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoteRow(
    note: KanbanNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ListItemShape.Single,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = shape,
    ) {
        Column(
            modifier = Modifier.padding(
                KanbanDefaults.cardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(note.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = note.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
