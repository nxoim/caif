package com.nxoim.caif.springs

import androidx.compose.animation.core.SpringSpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a bouncy spring preset with noticeable overshoot.
 *
 * @param duration The perceptual duration of the spring motion. Defaults to 500ms.
 * @param extraBounce Additional bounce to add to the preset base bounce of `0.35f`.
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
@Suppress("UNCHECKED_CAST")
fun <T> bouncy(
    duration: Duration = defaultDuration,
    extraBounce: Float = 0.0f,
    visibilityThreshold: T? = null
): SpringSpec<T> =
    if (duration == defaultDuration && extraBounce == 0.0f && visibilityThreshold == null) {
        DefaultBouncySpec as SpringSpec<T>
    } else {
        springA(
            duration = duration,
            bounce = 0.35f + extraBounce,
            visibilityThreshold = visibilityThreshold
        )
    }

/**
 * Creates a smooth spring preset with minimal, gentle overshoot.
 *
 * @param duration The perceptual duration of the spring motion. Defaults to 500ms.
 * @param extraBounce Additional bounce to add to the preset base bounce of `0.05f`.
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
@Suppress("UNCHECKED_CAST")
fun <T> smooth(
    duration: Duration = defaultDuration,
    extraBounce: Float = 0.0f,
    visibilityThreshold: T? = null
): SpringSpec<T> =
    if (duration == defaultDuration && extraBounce == 0.0f && visibilityThreshold == null) {
        DefaultSmoothSpec as SpringSpec<T>
    } else {
        springA(
            duration = duration,
            bounce = (0.05f + extraBounce),
            visibilityThreshold = visibilityThreshold
        )
    }

/**
 * Creates a snappy spring preset with quick response and moderate bounce.
 *
 * @param duration The perceptual duration of the spring motion. Defaults to 500ms.
 * @param extraBounce Additional bounce to add to the preset base bounce of `0.15f`.
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
@Suppress("UNCHECKED_CAST")
fun <T> snappy(
    duration: Duration = defaultDuration,
    extraBounce: Float = 0.0f,
    visibilityThreshold: T? = null
): SpringSpec<T> =
    if (duration == defaultDuration && extraBounce == 0.0f && visibilityThreshold == null) {
        DefaultSnappySpec as SpringSpec<T>
    } else {
        springA(
            duration = duration,
            bounce = (0.15f + extraBounce),
            visibilityThreshold = visibilityThreshold
        )
    }

/**
 * Creates a short, interactive spring tuned for immediate touch/drag feedback.
 *
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
@Suppress("UNCHECKED_CAST")
fun <T> interactiveSpring(
    visibilityThreshold: T? = null
): SpringSpec<T> = if (visibilityThreshold == null) {
        DefaultInteractiveSpec as SpringSpec<T>
    } else {
        springA(
            duration = interactiveDuration,
            visibilityThreshold = visibilityThreshold
        )
    }


private val defaultDuration = 500.milliseconds
private val interactiveDuration = 150.milliseconds

private val DefaultBouncySpec: SpringSpec<Any> = springA(
    duration = defaultDuration,
    bounce = 0.35f,
    visibilityThreshold = null
)

private val DefaultSmoothSpec: SpringSpec<Any> = springA(
    duration = defaultDuration,
    bounce = 0.05f,
    visibilityThreshold = null
)

private val DefaultSnappySpec: SpringSpec<Any> = springA(
    duration = defaultDuration,
    bounce = 0.15f,
    visibilityThreshold = null
)

private val DefaultInteractiveSpec: SpringSpec<Any> = springA(
    duration = interactiveDuration,
    bounce = 0f,
    visibilityThreshold = null
)