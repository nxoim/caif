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
        stopOnTargetReached: Boolean = false,
    )
}

abstract class AbstractMutableAnimatedValue<Value, Vector : AnimationVector>(
    converter: TwoWayConverter<Value, Vector>,
    protected val zeroVelocity: Value,
    initialValue: Value,
    private val label: String = "AnimatedValue",
) : MutableAnimatedValue<Value> {
    protected val animatable = Animatable(initialValue, converter, label = label)
    private var activeJob: Job? = null
    private var generation = 0

    protected var snapped by mutableStateOf(false)
        private set

    protected fun beginDirectWrite() {
        activeJob?.cancel()
        activeJob = null
        generation++
    }

    protected fun finishDirectWrite() {
        snapped = true
    }

    protected abstract suspend fun syncSnapStateToAnimatable()

    protected abstract fun updateVelocityState(value: Value)

    protected abstract fun clearVelocityState()

    protected open fun hasCrossedTarget(
        current: Value,
        target: Value,
        start: Value,
    ): Boolean = false

    final override suspend fun animateTo(
        target: Value,
        spec: AnimationSpec<Value>,
        initialVelocity: Value,
        stopOnTargetReached: Boolean
    ) {
        activeJob?.cancel()

        val job = currentCoroutineContext()[Job]
        activeJob = job

        val gen = ++generation

        if (snapped) {
            syncSnapStateToAnimatable()
            snapped = false
        }

        val startValue = animatable.value

        val effectiveVelocity = if (initialVelocity != zeroVelocity) {
            initialVelocity
        } else {
            animatable.velocity
        }

        var crossedTarget = false

        try {
            animatable.animateTo(
                targetValue = target,
                animationSpec = spec,
                initialVelocity = effectiveVelocity,
            ) {
                updateVelocityState(velocity)

                if (
                    stopOnTargetReached &&
                    hasCrossedTarget(
                        current = animatable.value,
                        target = target,
                        start = startValue
                    )
                ) {
                    crossedTarget = true
                    throw TargetReachedCancellation
                }
            }
        } catch (cancellation: CancellationException) {
            if (cancellation !== TargetReachedCancellation) throw cancellation
        } finally {
            if (gen == generation) {
                if (crossedTarget) animatable.snapTo(target)

                clearVelocityState()

                if (activeJob === job) activeJob = null
            }
        }
    }
}

class GenericMutableAnimatedValue<Value, Vector : AnimationVector>(
    converter: TwoWayConverter<Value, Vector>,
    zeroVelocity: Value,
    initialValue: Value,
    label: String,
) : AbstractMutableAnimatedValue<Value, Vector>(
    converter = converter,
    zeroVelocity = zeroVelocity,
    initialValue = initialValue,
    label = label,
) {
    private val snapState = mutableStateOf(initialValue)
    private val velocityState = mutableStateOf(zeroVelocity)

    constructor(
        converter: TwoWayConverter<Value, Vector>,
        zeroVelocity: Value,
        initialValue: () -> Value,
        label: String = "AnimatedValue",
    ) : this(
        converter = converter,
        zeroVelocity = zeroVelocity,
        initialValue = initialValue(),
        label = label,
    )

    override var value: Value
        get() =
            if (snapped) {
                snapState.value
            } else {
                animatable.value
            }
        set(newValue) {
            beginDirectWrite()

            velocityState.value = zeroVelocity
            snapState.value = newValue

            finishDirectWrite()
        }

    override val velocity: Value
        get() = velocityState.value

    override suspend fun syncSnapStateToAnimatable() {
        animatable.snapTo(snapState.value)
    }

    override fun updateVelocityState(value: Value) {
        velocityState.value = value
    }

    override fun clearVelocityState() {
        velocityState.value = zeroVelocity
    }
}


interface TargetableMutableAnimatedValue<Value, Context> : AnimatedValue<Value> {
    override var value: Value

    fun snapToTarget(target: Context)

    fun prepareVelocity(new: Value)

    suspend fun animateTo(target: Context)
}

fun <Value, Context> TargetableMutableAnimatedValue(
    base: MutableAnimatedValue<Value>,
    valueMapper: Context.() -> Value,
    specFactory: Context.() -> AnimationSpec<Value>,
    stopOnTargetReached: (Context.() -> Boolean)? = null,
): TargetableMutableAnimatedValue<Value, Context> =
    object : TargetableMutableAnimatedValue<Value, Context> {

        private var preparedVelocity: Value? = null

        override var value: Value
            get() = base.value
            set(newValue) {
                base.value = newValue
                preparedVelocity = null
            }

        override val velocity: Value
            get() = base.velocity

        override fun snapToTarget(target: Context) {
            base.value = valueMapper(target)
            preparedVelocity = null
        }

        override fun prepareVelocity(new: Value) {
            preparedVelocity = new
        }

        override suspend fun animateTo(target: Context) {
            val actualTarget = valueMapper(target)
            val spec = specFactory(target)
            val initialVelocity = preparedVelocity ?: velocity
            val stop = stopOnTargetReached?.invoke(target) ?: false

            preparedVelocity = null

            base.animateTo(
                target = actualTarget,
                spec = spec,
                initialVelocity = initialVelocity,
                stopOnTargetReached = stop,
            )
        }
    }


fun <Value> MutableAnimatedValue<Value>.toAnimatedValue(): AnimatedValue<Value> =
    object : AnimatedValue<Value> {
        override val value get() = this@toAnimatedValue.value

        override val velocity get() = this@toAnimatedValue.velocity
    }

fun <Value, Context> TargetableMutableAnimatedValue<Value, Context>.toAnimatedValue(): AnimatedValue<Value> =
    object : AnimatedValue<Value> {
        override val value  get() = this@toAnimatedValue.value

        override val velocity get() = this@toAnimatedValue.velocity
    }

private object TargetReachedCancellation : CancellationException("Crossed target")
