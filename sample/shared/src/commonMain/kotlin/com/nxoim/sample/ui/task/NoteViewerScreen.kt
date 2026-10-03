@file:OptIn(ExperimentalAnimationApi::class)

package com.nxoim.sample.ui.task

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.ui.common.ErrorState
import com.nxoim.sample.ui.common.LoadState
import com.nxoim.sample.ui.common.LoadingState
import com.nxoim.sample.ui.common.NotFoundState
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds
import kotlinx.coroutines.flow.StateFlow

internal interface NoteController {
    val note: StateFlow<LoadState<KanbanNote, String>>

    fun updateTitle(title: String): Boolean
    fun updateText(text: String): Boolean
}

@Composable
internal fun NoteViewerScreen(
    controller: NoteController,
    sharedElementKey: Any? = null,
    onBack: () -> Unit,
) {
    val state by controller.note.collectAsState()

    updateTransition(state, "NoteViewerScreen").Crossfade(
        contentKey = { it::class },
        modifier = Modifier
            .fillMaxSize()
            .sharedBounds(sharedElementKey)
    ) { note ->
        when (note) {
            is LoadState.Loading -> LoadingState()
            is LoadState.NotFound -> NotFoundState(entity = "Note")
            is LoadState.Error -> ErrorState(entity = "note", cause = note.cause)
            is LoadState.Content -> {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Note") },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextField(
                            value = note.value.title,
                            onValueChange = controller::updateTitle,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Title", style = MaterialTheme.typography.titleLarge)
                            },
                            textStyle = MaterialTheme.typography.titleLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                        )
                        TextField(
                            value = note.value.text,
                            onValueChange = controller::updateText,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Note text…", style = MaterialTheme.typography.bodyLarge)
                            },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            minLines = 8,
                            maxLines = 16,
                        )
                    }
                }
            }
        }
    }
}
