package com.nxoim.caif.utils

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class TypeMap private constructor(
    private val map: MutableScatterMap<KClass<*>, Any>
) {
    constructor() : this(mutableScatterMapOf())

    fun <T : Any> put(type: KClass<T>, instance: T) {
        map[type] = instance
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): T? = map[type] as? T

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPut(type: KClass<T>, factory: () -> T) = map.getOrPut(type, factory) as T

    fun <T : Any> remove(type: KClass<T>): Boolean =
        map.remove(type) != null

    operator fun contains(type: KClass<*>): Boolean = type in map

    val size: Int get() = map.size
    fun isEmpty(): Boolean = map.isEmpty()
    fun isNotEmpty(): Boolean = map.isNotEmpty()
    fun clear() = map.clear()

    inline fun <reified T : Any> put(instance: T) = put(T::class, instance)
    inline fun <reified T : Any> get(): T? = get(T::class)
    inline fun <reified T : Any> getOrPut(noinline factory: () -> T): T = getOrPut(T::class, factory)
    inline fun <reified T : Any> remove() = remove(T::class)
    inline fun <reified T : Any> contains(): Boolean = contains(T::class)
}

@PublishedApi
internal fun typeMap() = TypeMap()
