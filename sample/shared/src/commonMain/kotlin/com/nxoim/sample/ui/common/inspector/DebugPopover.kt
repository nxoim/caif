package com.nxoim.sample.ui.common.inspector

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DebugPopover(
    modifier: Modifier = Modifier,
    sharedElementsEnabled: Boolean = true,
    onToggleSharedElements: (Boolean) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    SharedTransitionLayout(modifier) {
        AnimatedContent(
            targetState = isExpanded,
            contentAlignment = Alignment.BottomStart,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "DebugPopoverState",
        ) { expanded ->
            if (!expanded) {
                Surface(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(Unit),
                            animatedVisibilityScope = this@AnimatedContent,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                        )
                        .size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shadowElevation = 2.dp,
                ) {
                    IconButton(
                        onClick = { isExpanded = true },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(Unit),
                            animatedVisibilityScope = this@AnimatedContent,
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.End),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "Shared Elements",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                Switch(
                                    checked = sharedElementsEnabled,
                                    onCheckedChange = onToggleSharedElements,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
