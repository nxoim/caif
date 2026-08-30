@file:OptIn(ExperimentalAnimationApi::class)

package com.nxoim.sample.ui.task

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nxoim.caif.decompose.DecomposeStack
import com.nxoim.caif.decompose.adaptiveStackAnimation
import com.nxoim.caif.decompose.decomposeAnimations
import com.nxoim.evolpagink.compose.toState
import com.nxoim.evolpagink.core.Pageable
import com.nxoim.sample.model.KanbanNote
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.ErrorState
import com.nxoim.sample.ui.common.LoadState
import com.nxoim.sample.ui.common.LoadingState
import com.nxoim.sample.ui.common.NotFoundState
import com.nxoim.sample.ui.common.SharedElementKeyFactory
import com.nxoim.sample.ui.common.expansionSwipeStackAnimation
import com.nxoim.sample.ui.common.sharedtransition.LocalAnimatedVisibilityScope
import com.nxoim.sample.ui.common.sharedtransition.LocalSharedElementsEnabled
import com.nxoim.sample.ui.common.sharedtransition.sharedBounds
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun TaskDetailsStackHost(
    sharedElementKey: Any,
    sharedElementKeys: SharedElementKeyFactory,
    component: TaskDetailsComponent,
    controller: TaskDetailsController,
) {
    val taskState by controller.task.collectAsState()
    val noteListState = rememberLazyListState()
    val notes = controller.notes.toState(noteListState)
    val useSharedElements = LocalSharedElementsEnabled.current

    updateTransition(taskState, label = "TaskStateTransition").Crossfade(
        contentKey = { it::class },
    ) { state ->
        when (state) {
            LoadState.Loading -> LoadingState()
            LoadState.NotFound -> NotFoundState(entity = "Task")
            is LoadState.Error -> ErrorState(entity = "task", cause = state.cause)
            is LoadState.Content -> {
                val task = state.value
                DecomposeStack(
                    stack = component.stack,
                    backHandler = component.backHandler,
                    onPop = component.navigation::navigateBack,
                    modifier = Modifier.sharedBounds(sharedElementKey),
                    animationFactory = remember(useSharedElements) {
                        decomposeAnimations { child ->
                            when (child) {
                                TaskChild.Details -> adaptiveStackAnimation()
                                is TaskChild.Note -> if (useSharedElements)
                                    expansionSwipeStackAnimation()
                                else
                                    adaptiveStackAnimation()
                            }
                        }
                    },
                ) { child ->
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        when (child) {
                            TaskChild.Details -> TaskDetailContent(
                                task = task,
                                notes = notes,
                                noteListState = noteListState,
                                provideNoteSharedKey = { note ->
                                    sharedElementKeys.note(
                                        task.id,
                                        note.id,
                                    )
                                },
                                onBack = component.navigation::navigateBack,
                                onNote = { noteId ->
                                    component.navigation.openNote(noteId)
                                },
                            )

                            is TaskChild.Note -> NoteViewerScreen(
                                controller = child.component.model,
                                provideSharedKey = { note ->
                                    sharedElementKeys.note(task.id, note.id)
                                },
                                onBack = component.navigation::navigateBack,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal interface TaskDetailsController {
    val task: StateFlow<LoadState<KanbanTask>>
    val notes: Pageable<Int, KanbanNote>
}

