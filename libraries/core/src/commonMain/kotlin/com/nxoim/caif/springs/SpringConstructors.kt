package com.nxoim.caif.springs

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a [SpringSpec] parameterized by [duration] and [bounce].
 *
 * @param duration The perceptual duration or period of oscillation. Must be positive.
 * @param bounce The bounciness factor in range `[-1.0f, 1.0f]`. `0.0f` is critically damped
 *   (no overshoot), `> 0.0f` increases bounciness (underdamped), and `< 0.0f` increases
 *   resistance to oscillation (overdamped).
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
fun <T> springA(
    duration: Duration = 500.milliseconds,
    bounce: Float = 0f,
    visibilityThreshold: T? = null
): SpringSpec<T> {
    require(duration.isPositive()) { "duration must be positive" }
    require(bounce.isFinite() && bounce in -1.0f..1.0f) { "bounce must be finite and within -1.0f..1.0f, was $bounce" }
    // -1.0 -> 2.0 (overdamped)
    //  0.0 -> 1.0 (critically damped)
    //  1.0 -> 0.0 (undamped)
    val dampingRatio = when {
        bounce >= 0f -> 1.0f - bounce
        else -> 1.0f + abs(bounce)
    }

    val periodSeconds = duration.inWholeNanoseconds / 1000000000f
    val undampedNaturalFrequency = (2f * piFloat) / periodSeconds // ωn = 2π / T

    return spring(
        stiffness = undampedNaturalFrequency.pow(2),
        dampingRatio = dampingRatio,
        visibilityThreshold = visibilityThreshold
    )
}

/**
 * Creates a [SpringSpec] parameterized by [response] time and [dampingFraction].
 *
 * @param response The duration in seconds for approximately one full oscillation cycle (`2π / ω`).
 *   Must be positive and finite.
 * @param dampingFraction The damping ratio (`1.0f` for critically damped, `< 1.0f` for bouncy,
 *   `> 1.0f` for overdamped). Must be non-negative and finite.
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
fun <T> springB(
    response: Float = 0.5f,
    dampingFraction: Float = 0.825f,
    visibilityThreshold: T? = null
): SpringSpec<T> {
    require(response > 0f && response.isFinite()) { "response must be positive and finite, was $response" }
    require(dampingFraction >= 0f && dampingFraction.isFinite()) { "dampingFraction must be non-negative and finite, was $dampingFraction" }

    return spring(
        stiffness = (2 * piFloat / response).pow(2),
        dampingRatio = dampingFraction,
        visibilityThreshold = visibilityThreshold
    )
}

/**
 * Creates a [SpringSpec] from physical mass-spring-damper system constants.
 *
 * @param mass The mass of the object in motion. Must be positive and finite.
 * @param stiffness The stiffness coefficient (spring constant k). Must be positive and finite.
 * @param damping The damping coefficient (drag/friction c). Must be non-negative and finite.
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
fun <T> springC(
    mass: Float,
    stiffness: Float,
    damping: Float,
    visibilityThreshold: T? = null
): SpringSpec<T> {
    require(mass > 0f && mass.isFinite()) { "mass must be positive and finite, was $mass" }
    require(stiffness > 0f && stiffness.isFinite()) { "stiffness must be positive and finite, was $stiffness" }
    require(damping >= 0f && damping.isFinite()) { "damping must be non-negative and finite, was $damping" }

    // this reads misleading but the result is correct
    return spring(
        dampingRatio = damping / (2f * sqrt(mass * stiffness)),
        stiffness = sqrt(stiffness / mass),
        visibilityThreshold = visibilityThreshold
    )
}

/**
 * Creates a [SpringSpec] parameterized by [settlingDuration] and [dampingRatio].
 *
 * @param settlingDuration The duration within which the spring oscillation settles. Must be positive.
 * @param dampingRatio The damping ratio of the system. Must be positive and finite.
 * @param visibilityThreshold Optional threshold below which the animation is considered settled.
 */
fun <T> springD(
    settlingDuration: Duration,
    dampingRatio: Float,
    visibilityThreshold: T? = null
): SpringSpec<T> {
    require(settlingDuration.isPositive()) { "settlingDuration must be positive" }
    require(dampingRatio > 0f && dampingRatio.isFinite()) { "dampingRatio must be positive and finite, was $dampingRatio" }

    return spring(
        stiffness = (2f * piFloat / (settlingDuration.inWholeMilliseconds / 1000f)).pow(2) / (dampingRatio.pow(2)),
        dampingRatio = dampingRatio,
        visibilityThreshold = visibilityThreshold
    )
}

private const val piFloat = PI.toFloat()