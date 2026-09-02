package com.nxoim.caif.core

import androidx.collection.ScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

/**
 * Chooses and retains the animation used by a selectable item animation.
 *
 * Implementations are stateful. A new strategy instance is created for every item animation unit.
 */
interface AnimationSelectorStrategy {
    val lastSelected: AnimationSelector?

    fun select(
        requested: AnimationSelector?,
        defaultSelector: AnimationSelector
    ): AnimationSelector
}

/** Selects the requested animation, or the declared default when no animation was requested. */
class LastSelectedAnimationSelectorStrategy : AnimationSelectorStrategy {
    private var selected by mutableStateOf<AnimationSelector?>(null)

    override val lastSelected: AnimationSelector?
        get() = selected

    override fun select(
        requested: AnimationSelector?,
        defaultSelector: AnimationSelector
    ): AnimationSelector = (requested ?: defaultSelector).also { selected = it }
}

/**
 * Builds alternative complete item animations selected by input capability.
 *
 * [selectorStrategyFactory] is invoked once per item so its state is never shared between items.
 */
fun <Context> buildSelectableItemAnimation(
    selectorStrategyFactory: () -> AnimationSelectorStrategy =
        { LastSelectedAnimationSelectorStrategy() },
    block: SelectableItemAnimationBuilder<Context>.() -> Unit
): ItemAnimation<Context> {
    val builder = SelectableItemAnimationBuilder<Context>()
    builder.block()
    return builder.build(selectorStrategyFactory())
}

@AnimationDsl
class SelectableItemAnimationBuilder<Context> internal constructor() {
    private val animations = mutableScatterMapOf<AnimationSelector, ItemAnimation<Context>>()
    private val selectorsByCapability = mutableScatterMapOf<KClass<*>, AnimationSelector>()
    private var declaredDefaultSelector: AnimationSelector? = null

    inline fun <reified Capability : Any> selectOnCapability(
        noinline animation: () -> ItemAnimation<Context>
    ): AnimationSelector = selectOnCapability(Capability::class, animation)

    fun <Capability : Any> selectOnCapability(
        capabilityType: KClass<Capability>,
        animation: () -> ItemAnimation<Context>
    ): AnimationSelector {
        require(capabilityType !in selectorsByCapability) {
            "An animation is already selected for capability ${capabilityType.simpleName}."
        }

        val unit = animation()
        require(unit.getAndSelectCapability(capabilityType) != null) {
            "The animation selected for ${capabilityType.simpleName} must register that capability."
        }

        val selector = AnimationSelector(animations.size)
        animations[selector] = unit
        selectorsByCapability[capabilityType] = selector
        return selector
    }

    fun defaultSelector(selector: AnimationSelector) {
        require(selector in animations) {
            "The default selector must belong to this selectable item animation."
        }
        declaredDefaultSelector = selector
    }

    internal fun build(strategy: AnimationSelectorStrategy): ItemAnimation<Context> {
        require(animations.isNotEmpty()) {
            "A selectable item animation must declare at least one animation."
        }
        val defaultSelector = declaredDefaultSelector
            ?: if (animations.size == 1) {
                animations.asMap().keys.first()
            } else {
                throw IllegalArgumentException(
                    "A selectable item animation with multiple animations must declare a default selector."
                )
            }
        return SelectableItemAnimationImpl(
            animations = animations.asMap(),
            selectorsByCapability = selectorsByCapability,
            defaultSelector = defaultSelector,
            strategy = strategy
        )
    }
}

internal interface SelectableItemAnimation<Context> : ItemAnimation<Context> {
    fun selectAnimationForCapability(capabilityType: KClass<*>)
    fun selectDefaultAnimation()
}

private class SelectableItemAnimationImpl<Context>(
    private val animations: Map<AnimationSelector, ItemAnimation<Context>>,
    private val selectorsByCapability: ScatterMap<KClass<*>, AnimationSelector>,
    private val defaultSelector: AnimationSelector,
    private val strategy: AnimationSelectorStrategy
) : SelectableItemAnimation<Context> {
    private var hasLogicalContext = false
    private var logicalContext: Context? = null
    private var selectedSelector by mutableStateOf<AnimationSelector?>(null)

    init {
        select(requested = null)
    }

    override val modifier: Modifier
        get() = selectedAnimation.modifier

    override fun willBeVisible(context: Context): Boolean =
        selectedAnimation.willBeVisible(context)

    override fun reset(context: Context) {
        logicalContext = context
        hasLogicalContext = true
        animations.values.forEach { it.reset(context) }
    }

    override suspend fun animateTo(target: Context) {
        logicalContext = target
        hasLogicalContext = true
        selectedAnimation.animateTo(target)
    }

    override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? {
        selectAnimationForCapability(kClass)
        return selectedAnimation.getAndSelectCapability(kClass)
    }

    override fun selectAnimationForCapability(capabilityType: KClass<*>) {
        select(selectorsByCapability[capabilityType])
    }

    override fun selectDefaultAnimation() {
        select(requested = null)
    }

    private fun select(requested: AnimationSelector?) {
        val previous = selectedSelector
        val selected = strategy.select(requested, defaultSelector)
        require(selected in animations) {
            "AnimationSelectorStrategy selected an animation from another item animation."
        }
        require(strategy.lastSelected == selected) {
            "AnimationSelectorStrategy must retain the animation returned by select."
        }
        selectedSelector = selected

        if (selected != previous && hasLogicalContext) {
            @Suppress("UNCHECKED_CAST")
            animations.getValue(selected).reset(logicalContext as Context)
        }
    }

    private val selectedAnimation: ItemAnimation<Context>
        get() {
            val selector = requireNotNull(selectedSelector) {
                "A selectable item animation has no selected animation."
            }
            return animations.getValue(selector)
        }
}

/** Identifies one complete animation declared in a selectable item animation. */
@JvmInline
value class AnimationSelector internal constructor(internal val index: Int)