package com.nxoim.sample.ui.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
internal fun ReviewActions(
    enabled: Boolean,
    onNotDone: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fadeHeight = with(density) { ReviewDefaults.chromeFadeHeight.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0f),
                        surfaceColor,
                    ),
                    startY = 0f,
                    endY = fadeHeight,
                ),
            )
            .navigationBarsPadding()
            .padding(
                top = ReviewDefaults.chromeFadeHeight,
                bottom = 16.dp,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = ReviewDefaults.contentMaximumWidth)
                .fillMaxWidth()
                .padding(horizontal = ReviewDefaults.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onNotDone,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Not done yet")
            }
            Button(
                onClick = onDone,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Done")
            }
        }
    }
}
