package com.nxoim.caif.prefabs.stack

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory

class ItemAnimationRegistry<ItemType, Key : Any, Context>(
    private val factory: ItemAnimationFactory<ItemType, Key, Context>
) {
    private val _animations: MutableScatterMap<Key, ItemAnimation<Context>> = mutableScatterMapOf()

    val animations: Map<Key, ItemAnimation<Context>>
        get() = MutableScatterMap<Key, ItemAnimation<Context>>(_animations.capacity)
            .apply { putAll(_animations) }
            .asMap()

    fun getOrCreate(item: ItemType, key: Key, initialContext: () -> Context): ItemAnimation<Context> =
        _animations.getOrPut(key) {
            factory.create(item, key).apply { reset(initialContext()) }
        }

    fun evict(key: Key) {
        _animations.remove(key)
    }

    internal fun clear() {
        _animations.clear()
    }
}
