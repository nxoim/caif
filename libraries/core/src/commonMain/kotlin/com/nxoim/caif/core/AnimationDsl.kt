package com.nxoim.caif.core

import com.nxoim.caif.core.base.TargetableMutableAnimatedValue

interface AnimationBuilder<Context> {
    fun <ValueType> registerAnimation(block: () -> TargetableMutableAnimatedValue<ValueType, Context>): TargetableMutableAnimatedValue<ValueType, Context>
    @DelicateAnimationBuilderAnimationCall
    fun animateTo(block: suspend Context.() -> Unit)
}


@DslMarker
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.TYPE
)
annotation class AnimationDsl

@Suppress("ExperimentalAnnotationRetention")
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "`animateTo` bypasses the registered animations system. You are responsible for animating all registered values manually, otherwise they will remain frozen.",
    level = RequiresOptIn.Level.WARNING,
)
annotation class DelicateAnimationBuilderAnimationCall