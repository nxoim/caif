package com.nxoim.sample.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nxoim.sample.model.KanbanTask
import com.nxoim.sample.model.TaskStatus
import com.nxoim.sample.ui.common.sharedtransition.LocalSharedTransitionScope

@Composable
internal fun TaskRow(
    task: KanbanTask,
    modifier: Modifier = Modifier,
    shape: Shape = ListItemShape.Single,
    onClick: () -> Unit,
) {
    val isDone = task.status == TaskStatus.Done
    val textDecoration = if (isDone) TextDecoration.LineThrough else null

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .run {
                    val scope = LocalSharedTransitionScope.current
                    if (scope != null)
                        with(scope) { skipToLookaheadSize() }
                    else
                        this
                }
                .padding(KanbanDefaults.cardPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (isDone) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
                tint = if (isDone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = textDecoration,
                )
                Text(
                    text = task.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = textDecoration,
                )
            }
        }
    }
}
