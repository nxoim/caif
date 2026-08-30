package com.nxoim.caif.utils

import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
internal value class TypeSetMap private constructor(
    private val map: MutableMap<KClass<*>, MutableSet<Any>>
) {
    constructor() : this(mutableMapOf())

    fun <T : Any> put(type: KClass<T>, instance: T) {
        map.getOrPut(type) { mutableSetOf() }.add(instance)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): Set<T> =
        (map[type] as? Set<T>) ?: emptySet()

    fun <T : Any> remove(type: KClass<T>, instance: T): Boolean =
        map[type]?.remove(instance) == true

    fun forEachEntry(block: (KClass<*>, Any) -> Unit) {
        map.forEach { (type, set) -> set.forEach { block(type, it) } }
    }

    inline fun <reified T : Any> put(instance: T) = put(T::class, instance)
    inline fun <reified T : Any> get(): Set<T> = get(T::class)
    inline fun <reified T : Any> remove(instance: T) = remove(T::class, instance)
}

internal fun typeSetMap() = TypeSetMap()