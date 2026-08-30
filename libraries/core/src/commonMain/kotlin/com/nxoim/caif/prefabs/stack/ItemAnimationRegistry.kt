package com.nxoim.caif.prefabs.stack

import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory

class ItemAnimationRegistry<ItemType, Key : Any, Context>(
    private val factory: ItemAnimationFactory<ItemType, Key, Context>
) {
    val animations: Map<Key, ItemAnimation<Context>>
        field = mutableMapOf<Key, ItemAnimation<Context>>()

    fun getOrCreate(item: ItemType, key: Key, initialContext: () -> Context) =
        animations.getOrPut(key) {
            factory.create(item, key).apply { reset(initialContext()) }
        }

    fun evict(key: Key) {
        animations.remove(key)
    }

    internal fun clear() {
        animations.clear()
    }
}
