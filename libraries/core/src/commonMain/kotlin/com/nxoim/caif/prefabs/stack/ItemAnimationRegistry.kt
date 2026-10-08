package com.nxoim.caif.prefabs.stack

import androidx.collection.mutableScatterMapOf
import com.nxoim.caif.core.AnimationDefinition
import com.nxoim.caif.core.AnimationDefinitions
import com.nxoim.caif.core.DeclaredAnimationFactory
import com.nxoim.caif.core.DeclaredItemAnimation
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory
import kotlin.reflect.KClass

class ItemAnimationRegistry<ItemType, Key : Any, Context> private constructor(
    private val factoryProvider: () -> ItemAnimationFactory<ItemType, Key, Context>
) {
    constructor(factory: ItemAnimationFactory<ItemType, Key, Context>) : this({ factory })

    private val entries = mutableScatterMapOf<Key, Entry<ItemType, Key, Context>>()
    private var cycleFactory: ItemAnimationFactory<ItemType, Key, Context>? = null

    val animations: Map<Key, ItemAnimation<Context>>
        get() = buildMap(entries.size) {
            this@ItemAnimationRegistry.entries.forEach { key, entry -> put(key, entry.animation) }
        }

    fun getOrCreate(item: ItemType, key: Key, initialContext: () -> Context): ItemAnimation<Context> =
        getOrCreateDeclared(item, key, initialContext)

    fun evict(key: Key) {
        entries.remove(key)
    }

    internal companion object {
        fun <ItemType, Key : Any, Context> fromProvider(
            provider: () -> ItemAnimationFactory<ItemType, Key, Context>
        ): ItemAnimationRegistry<ItemType, Key, Context> = ItemAnimationRegistry(provider)
    }

    internal fun get(key: Key): DeclaredItemAnimation<Context>? = entries[key]?.animation

    internal fun beginCycle() {
        cycleFactory = factoryProvider()
    }

    internal fun sourceItem(inputCapability: KClass<*>?, stackSnapshot: List<ItemType>): ItemType? =
        factory().sourceItem(inputCapability, stackSnapshot)

    internal fun getOrCreateDeclared(
        item: ItemType,
        key: Key,
        initialContext: () -> Context,
    ): DeclaredItemAnimation<Context> {
        val factory = factory()
        val entry = entries.getOrPut(key) {
            Entry(factory, DeclaredItemAnimation(definitions(factory, item, key)).apply { reset(initialContext()) })
        }
        if (entry.factory != factory) {
            entry.animation.rebind(definitions(factory, item, key))
            entry.factory = factory
        }
        return entry.animation
    }

    internal fun clear() {
        entries.clear()
        cycleFactory = null
    }

    private fun factory(): ItemAnimationFactory<ItemType, Key, Context> =
        cycleFactory ?: factoryProvider().also { cycleFactory = it }

    private fun definitions(
        factory: ItemAnimationFactory<ItemType, Key, Context>,
        item: ItemType,
        key: Key
    ): AnimationDefinitions<Context> {
        if (factory is DeclaredAnimationFactory<ItemType, Key, Context>) return factory.definitions(item, key)

        val animation = factory.create(item, key)
        if (animation is DeclaredItemAnimation<Context>) return animation.definitions
        return AnimationDefinitions(
            inputs = emptyMap(),
            default = AnimationDefinition({ animation }),
            supportsSharedSelection = false,
        )
    }
}

private class Entry<ItemType, Key : Any, Context>(
    var factory: ItemAnimationFactory<ItemType, Key, Context>,
    val animation: DeclaredItemAnimation<Context>
)
