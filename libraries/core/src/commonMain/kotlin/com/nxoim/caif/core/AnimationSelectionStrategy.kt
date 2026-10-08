package com.nxoim.caif.core

import kotlin.reflect.KClass

/**
 * Returns a common source from the supplied snapshot, or
 * null for independent item selection.
 */
fun interface AnimationSelectionStrategy<Item : Any> {
    fun sourceItem(inputCapability: KClass<*>?, stackSnapshot: List<Item>): Item?
}

private val PerItemSelection = AnimationSelectionStrategy<Any> { _, _ -> null }

@Suppress("UNCHECKED_CAST")
fun <Item : Any> perItem(): AnimationSelectionStrategy<Item> =
    PerItemSelection as AnimationSelectionStrategy<Item>

/**
 * Empty target stacks have no common source.
 * Exiting items use their own declarations.
 */
fun <Item : Any> fromItem(select: (List<Item>) -> Item): AnimationSelectionStrategy<Item> =
    AnimationSelectionStrategy { _, snapshot ->
        if (snapshot.isEmpty()) null else select(snapshot)
    }

/**
 * Chooses and delegates once at each cycle's existing selection point.
 */
fun <Item : Any> chooseStrategy(
    choose: (KClass<*>?, List<Item>) -> AnimationSelectionStrategy<Item>,
): AnimationSelectionStrategy<Item> = AnimationSelectionStrategy { input, snapshot ->
    choose(input, snapshot).sourceItem(input, snapshot)
}
