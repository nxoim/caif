package com.nxoim.caif.core

import androidx.compose.ui.util.fastForEach
import com.nxoim.caif.core.base.TargetableMutableAnimatedValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Implements [AnimationBuilder] so all standard animation extensions ([animateFloat], [animateOffset],
 * [animateColor], etc.) can be called directly in the class body, automatically registering into this animation.
 *
 * Subclasses can compose other animations using [registerChild]
 */
abstract class BaseItemAnimation<Context> : ItemAnimation<Context>, AnimationBuilder<Context> {
    private val registeredValues = mutableListOf<TargetableMutableAnimatedValue<*, Context>>()
    private val registeredChildren = mutableListOf<ItemAnimation<Context>>()
    private var customAnimateTo: (suspend Context.() -> Unit)? = null
    protected var currentContext: Context? = null
        private set

    override fun <ValueType> registerAnimation(
        block: () -> TargetableMutableAnimatedValue<ValueType, Context>
    ): TargetableMutableAnimatedValue<ValueType, Context> {
        val value = block()
        registeredValues.add(value)
        return value
    }

    protected fun <T : ItemAnimation<Context>> registerChild(child: T): T {
        registeredChildren.add(child)
        return child
    }

    @DelicateAnimationBuilderAnimationCall
    override fun animateTo(block: suspend Context.() -> Unit) {
        customAnimateTo = block
    }

    final override fun reset(context: Context) {
        currentContext = context
        registeredValues.fastForEach { it.snapToTarget(context) }
        registeredChildren.fastForEach { it.reset(context) }
    }

    final override suspend fun animateTo(target: Context) {
        currentContext = target
        val custom = customAnimateTo

        if (custom != null) {
            target.custom()
        } else if (registeredValues.isNotEmpty() || registeredChildren.isNotEmpty()) {
            coroutineScope {
                registeredValues.fastForEach { value ->
                    launch { value.animateTo(target) }
                }

                registeredChildren.fastForEach { child ->
                    launch { child.animateTo(target) }
                }
            }
        }

        if (!willBeVisible(target)) {
            reset(target)
        }
    }

    open fun Context.isVisibleWhen(): Boolean = true

    final override fun willBeVisible(context: Context): Boolean = with(context) { isVisibleWhen() }

    @Suppress("UNCHECKED_CAST")
    final override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? {
        if (kClass.isInstance(this)) return this as T
        registeredChildren.fastForEach { child ->
            val capability = child.getAndSelectCapability(kClass)
            if (capability != null) return capability
        }
        return null
    }
}
