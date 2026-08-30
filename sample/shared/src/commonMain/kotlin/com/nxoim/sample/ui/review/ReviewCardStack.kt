package com.nxoim.sample.ui.review

import androidx.collection.LruCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.nxoim.caif.decompose.SwipeCapabilityDispatcher
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import com.nxoim.caif.prefabs.stack.getOrCreateDispatcher
import com.nxoim.caif.prefabs.stack.rememberStackAnimatorState
import com.nxoim.caif.swipeable.SwipeConstraint
import com.nxoim.caif.swipeable.SwipeDirection
import com.nxoim.caif.swipeable.swipeable
import com.nxoim.evolpagink.compose.PageableComposeState
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.ui.common.KanbanDefaults
import com.nxoim.sample.ui.review.components.CardAffectedItemsPolicy
import com.nxoim.sample.ui.review.components.CardAnimationEnvironment
import com.nxoim.sample.ui.review.components.CardContext
import com.nxoim.sample.ui.review.components.ReviewDefaults
import com.nxoim.sample.ui.review.components.cardAnimation
import com.nxoim.sample.ui.review.components.rememberCardContextResolver
import com.nxoim.sample.ui.review.components.runStabilization

@Composable
internal fun ReviewCardStack(
    pageableState: PageableComposeState<KanbanTask>,
    positions: LruCache<String, CardContext.Position>,
    onDecision: (taskId: String, isDone: Boolean) -> Boolean,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val animationEnvironment = rememberCardStackEnvironmentState()
        val resolver = rememberCardContextResolver(
            positions = positions,
            keyFor = remember { KanbanTask::id },
            animationEnvironment = animationEnvironment,
        )

        val animator = rememberStackAnimatorState(
            stack = pageableState.items,
            resolver = resolver,
            factory = { _, _ -> cardAnimation() },
            affectedItemsPolicy = remember { CardAffectedItemsPolicy() },
            renderOrder = remember { RenderOrderStrategy.byStackIndex() },
        )
        val swipeDispatcher = animator.getOrCreateDispatcher(::SwipeCapabilityDispatcher)
        val density = LocalDensity.current

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            emptyContent()

            animator.itemsToRender.fastForEach { (pair, animation) ->
                val taskId = pair.first
                val task = pair.second

                key(taskId) {
                    LaunchedEffect(animation) {
                        animation.runStabilization()
                    }

                    ReviewTaskCard(
                        task = task,
                        modifier = animation.modifier
                            .padding(horizontal = ReviewDefaults.horizontalPadding)
                            .widthIn(max = ReviewDefaults.contentMaximumWidth)
                            .fillMaxWidth()
                            .heightIn(
                                min = ReviewDefaults.cardMinimumHeight,
                                max = ReviewDefaults.cardMaximumHeight,
                            )
                            .swipeable(
                                detectionConstraint = SwipeConstraint.all(),
                                confirmationConstraint = SwipeConstraint.horizontal(),
                                onStart = { swipeDispatcher.onStart() },
                                onProgress = { delta, _, _ ->
                                    swipeDispatcher.onUpdate(delta)
                                },
                                onCancel = { velocity ->
                                    swipeDispatcher.onEnd(velocity)
                                },
                                onConfirm = { velocity, direction ->
                                    val isDone = direction is SwipeDirection.Cardinal.End
                                    onDecision(task.id, isDone)
                                    with(density) {
                                        swipeDispatcher.onEnd(velocity)
                                    }
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReviewTaskCard(
    task: KanbanTask,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = KanbanDefaults.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(KanbanDefaults.expandedCardPadding),
            verticalArrangement = Arrangement.spacedBy(KanbanDefaults.sectionSpacing),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Swipe left for Not done yet · right for Done",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.rememberCardStackEnvironmentState(): State<CardAnimationEnvironment> {
    val layoutDirection = LocalLayoutDirection.current
    return rememberUpdatedState(
        CardAnimationEnvironment(
            maxWidth = constraints.maxWidth.toFloat(),
            maxHeight = constraints.maxHeight.toFloat(),
            layoutDirection = layoutDirection,
        ),
    )
}
