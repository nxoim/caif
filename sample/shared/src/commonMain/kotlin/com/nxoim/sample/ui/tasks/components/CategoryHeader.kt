package com.nxoim.sample.ui.tasks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.sample.model.KanbanCategory

@Composable
fun CategoryHeader(
    category: KanbanCategory,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(category.title, style = MaterialTheme.typography.titleLarge)
            Text(category.subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onReview) {
            Icon(Icons.Default.Reviews, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Review")
        }
    }
}
