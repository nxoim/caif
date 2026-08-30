package com.nxoim.sample.ui.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nxoim.sample.ui.review.ReviewState

@Composable
internal fun ReviewAppBar(
    categoryTitle: String,
    onBack: () -> Unit,
    reviewState: ReviewState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    ),
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CenterAlignedTopAppBar(
            title = { Text(categoryTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )
        ReviewProgress(
            state = reviewState,
            modifier = Modifier
                .widthIn(max = ReviewDefaults.contentMaximumWidth)
                .fillMaxWidth()
                .padding(
                    start = ReviewDefaults.horizontalPadding,
                    end = ReviewDefaults.horizontalPadding,
                    bottom = 12.dp,
                ),
        )
    }
}

@Composable
private fun ReviewProgress(
    state: ReviewState,
    modifier: Modifier = Modifier,
) {
    val hasRemainingTasks = state.reviewed != state.total
    val progress = if (state.total == 0) 1f else state.reviewed.toFloat() / state.total

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.isLoading) "Preparing review" else "Review tasks",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (state.isLoading) {
                        "Loading the open tasks…"
                    } else {
                        "${state.reviewed} of ${state.total} reviewed"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!state.isLoading && hasRemainingTasks) {
                Text(
                    text = "Swipe to decide",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
