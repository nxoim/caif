package com.nxoim.caif.core.base

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

interface AnimatedValue<Value> {
    val value: Value
    val velocity: Value
}

interface MutableAnimatedValue<Value> : AnimatedValue<Value> {
    override var value: Value
    suspend fun animateTo(
        target: Value,
        spec: AnimationSpec<Value> = spring(),
        initialVelocity: Value = velocity,
        stopOnTargetReached: Boolean = false
    )
}

interface TargetableMutableAnimatedValue<Value, Context> : AnimatedValue<Value> {
    override var value: Value
    fun snapToTarget(target: Context)
    fun prepareVelocity(new: Value)
    suspend fun animateTo(target: Context)
}

fun <Value, Vector : AnimationVector, Context> TargetableMutableAnimatedValue(
    base: AbstractMutableAnimatedValue<Value, Vector>,
    valueMapper: Context.() -> Value,
    specFactory: Context.() -> AnimationSpec<Value>,
    stopOnTargetReached: (Context.() -> Boolean)? = null
): TargetableMutableAnimatedValue<Value, Context> =
    object : TargetableMutableAnimatedValue<Value, Context> {
        override var value
            get() = base.value
            set(newValue) {
                base.value = newValue
                preparedVelocity = null
            }

        override val velocity get() = base.velocity
        var preparedVelocity: Value? = null

        override fun snapToTarget(target: Context)  {
            base.value = valueMapper(target)
            preparedVelocity = null
        }

        override fun prepareVelocity(new: Value) {
            preparedVelocity = new
        }

        override suspend fun animateTo(target: Context) {
            val actualTarget = valueMapper(target)
            val spec = specFactory(target)
            val velocity = preparedVelocity ?: velocity
            val stop = stopOnTargetReached?.invoke(target) ?: false
            preparedVelocity = null
            base.animateTo(actualTarget, spec, velocity, stop)
        }
    }

/**
 * Exists to mitigate Animatable's limitations regarding
 * animating and snapping. The mitigations are based on manual
 * management of publicly exposed state.
 */
abstract class AbstractMutableAnimatedValue<Value, Vector : AnimationVector>(
    private val converter: TwoWayConverter<Value, Vector>,
    private val zeroVelocity: Value,
    initialValue: Value,
    private val label: String = "AnimatedValue",
) : MutableAnimatedValue<Value> {
    private val animatable = Animatable(initialValue, converter, label = label)
    private var activeJob: Job? = null
    private var generation = 0

    /**  Abstract to permit usage of optimized state holders to prevent boxing on snap */
    protected abstract var snapValue: Value
    private var snapped by mutableStateOf(false)

    /**  Abstract to permit usage of optimized state holders to prevent boxing on velocity updates */
    protected abstract var velocityState: Value
    override val velocity: Value get() = velocityState

    override var value: Value
        get() = if (snapped) snapValue else animatable.value
        set(newValue) {
            activeJob?.cancel()
            activeJob = null
            generation++
            velocityState = zeroVelocity
            snapValue = newValue
            snapped = true
        }

    protected open fun hasCrossedTarget(current: Value, target: Value, start: Value): Boolean = false

    override suspend fun animateTo(
        target: Value,
        spec: AnimationSpec<Value>,
        initialVelocity: Value,
        stopOnTargetReached: Boolean
    ) {
        activeJob?.cancel()
        val job = currentCoroutineContext()[Job]
        activeJob = job
        val gen = ++generation

        // sync
        if (snapped) {
            animatable.snapTo(snapValue)
            snapped = false
        }

        val startValue = animatable.value
        val effectiveVelocity = if (initialVelocity != zeroVelocity) initialVelocity else animatable.velocity

        var crossedTarget = false
        try {
            animatable.animateTo(target, spec, effectiveVelocity) {
                velocityState = velocity
                if (stopOnTargetReached && hasCrossedTarget(animatable.value, target, startValue)) {
                    crossedTarget = true
                    throw TargetReachedCancellation
                }
            }
        } catch (cancellation: CancellationException) {
            if (cancellation !== TargetReachedCancellation) throw cancellation
        } finally {
            if (gen == generation) {
                if (crossedTarget) {
                    animatable.snapTo(target)
                }
                velocityState = zeroVelocity
                if (activeJob === job) activeJob = null
            }
        }
    }
}

private object TargetReachedCancellation : CancellationException("Crossed target")

class GenericMutableAnimatedValue<Value, Vector : AnimationVector>(
    converter: TwoWayConverter<Value, Vector>,
    zeroVelocity: Value,
    initialValue: () -> Value,
    label: String = "AnimatedValue",
) : AbstractMutableAnimatedValue<Value, Vector>(
    converter,
    zeroVelocity,
    initialValue = initialValue(),
    label = label,
) {
    override var snapValue by mutableStateOf(zeroVelocity)
    override var velocityState by mutableStateOf(zeroVelocity)
}

fun <Value> MutableAnimatedValue<Value>.toAnimatedValue(): AnimatedValue<Value> =
    object : AnimatedValue<Value> {
        override val value get() = this@toAnimatedValue.value
        override val velocity get() = this@toAnimatedValue.velocity
    }

fun <Value, Context> TargetableMutableAnimatedValue<Value, Context>.toAnimatedValue(): AnimatedValue<Value> =
    object : AnimatedValue<Value> {
        override val value get() = this@toAnimatedValue.value
        override val velocity get() = this@toAnimatedValue.velocity
    }
