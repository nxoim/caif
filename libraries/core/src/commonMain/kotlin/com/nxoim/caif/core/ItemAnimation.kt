package com.nxoim.caif.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import kotlin.reflect.KClass

interface ItemAnimation<Context> {
    val modifier: Modifier

    fun reset(context: Context)
    suspend fun animateTo(target: Context)
    fun willBeVisible(context: Context): Boolean
    fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T?
}

inline fun <reified T : Any> ItemAnimation<*>.getAndSelectCapability(): T? =
    getAndSelectCapability(T::class)

@Immutable
fun interface ItemAnimationFactory<in ItemType, in Key : Any, Context> {
    fun create(item: ItemType, key: Key): ItemAnimation<Context>
}
