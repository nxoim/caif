package com.nxoim.sample.ui.theme

import androidx.compose.animation.core.SpringSpec
import com.nxoim.caif.springs.smooth
import com.nxoim.caif.springs.snappy
import kotlin.time.Duration.Companion.seconds

fun <T> fastSmoothSpring(
    visibilityThreshold: T? = null,
): SpringSpec<T> = smooth(
    duration = fastDuration,
    visibilityThreshold = visibilityThreshold,
)

fun <T> fastSpring(
    visibilityThreshold: T? = null,
): SpringSpec<T> = snappy(
    duration = fastDuration,
    visibilityThreshold = visibilityThreshold,
)

private val fastDuration = 0.3.seconds
