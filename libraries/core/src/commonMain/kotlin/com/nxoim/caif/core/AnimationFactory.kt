package com.nxoim.caif.core

import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.reflect.KClass

@RememberInComposition
fun <Item : Any, Key : Any, Context> buildAnimationFactory(
    selectionStrategy: AnimationSelectionStrategy<Item> = perItem(),
    fallback: (AnimationDeclarations<Context>.() -> Unit)? = null,
    declarations: AnimationDeclarations<Context>.(Item, Key) -> Unit,
): ItemAnimationFactory<Item, Key, Context> {
    val fallbackDefinitions = fallback?.let { AnimationDeclarations<Context>().apply(it).build() }
    val fallbackDeclarations: (AnimationDeclarations<Context>.() -> Unit)? = fallbackDefinitions?.let { definitions ->
        { include(definitions) }
    }
    return object : DeclaredAnimationFactory<Item, Key, Context> {
        override val fallback = fallbackDeclarations

        override fun sourceItem(inputCapability: KClass<*>?, stackSnapshot: List<Item>): Item? =
            selectionStrategy.sourceItem(inputCapability, stackSnapshot)

        override fun definitions(item: Item, key: Key): AnimationDefinitions<Context> {
            val builder = AnimationDeclarations<Context>()
            builder.declarations(item, key)
            return builder.build(fallbackDefinitions)
        }
    }
}

internal interface DeclaredAnimationFactory<Item, Key : Any, Context> : ItemAnimationFactory<Item, Key, Context> {
    fun definitions(item: Item, key: Key): AnimationDefinitions<Context>

    override fun create(item: Item, key: Key): ItemAnimation<Context> =
        DeclaredItemAnimation(definitions(item, key))
}

/**
 * A handle to a declared animation constructor.
 */
class AnimationDefinition<Context> internal constructor(
    private val create: () -> ItemAnimation<Context>,
    private val inputCapability: KClass<*>? = null,
) {
    internal fun createInstance(): ItemAnimation<Context> = create().also { animation ->
        require(inputCapability == null || animation.getAndSelectCapability(inputCapability) != null) {
            "The animation declared for ${inputCapability?.simpleName} must provide that capability."
        }
    }
}

@AnimationDsl
class AnimationDeclarations<Context> internal constructor() {
    private val inputs = mutableMapOf<KClass<*>, AnimationDefinition<Context>>()
    private var default: AnimationDefinition<Context>? = null

    inline fun <reified Input : Any> onInput(
        noinline animation: () -> ItemAnimation<Context>,
    ): AnimationDefinition<Context> = onInput(Input::class, animation)

    fun onInput(
        inputCapability: KClass<*>,
        animation: () -> ItemAnimation<Context>,
    ): AnimationDefinition<Context> {
        require(inputCapability !in inputs) { "An animation is already declared for ${inputCapability.simpleName}." }
        return AnimationDefinition(animation, inputCapability).also { inputs[inputCapability] = it }
    }

    fun defaultAnimation(definition: AnimationDefinition<Context>) {
        require(inputs.values.any { it === definition }) { "The default must belong to this item's declarations." }
        default = definition
    }

    fun defaultAnimation(animation: () -> ItemAnimation<Context>) {
        default = AnimationDefinition(animation)
    }

    internal fun include(definitions: AnimationDefinitions<Context>) {
        definitions.inputs.forEach { (input, definition) ->
            require(input !in inputs) { "An animation is already declared for ${input.simpleName}." }
            inputs[input] = definition
        }
        default = definitions.default
    }

    internal fun build(fallback: AnimationDefinitions<Context>? = null): AnimationDefinitions<Context> {
        if (inputs.isEmpty() && default == null && fallback != null) return fallback

        return AnimationDefinitions(
            inputs = if (fallback == null)
                inputs.toMap()
            else
                buildMap(fallback.inputs.size + inputs.size) {
                    putAll(fallback.inputs)
                    putAll(inputs)
                },
            default = requireNotNull(default ?: fallback?.default) {
                "Declare a default animation for ordinary navigation and unmapped inputs."
            },
        )
    }
}

internal class AnimationDefinitions<Context>(
    val inputs: Map<KClass<*>, AnimationDefinition<Context>>,
    val default: AnimationDefinition<Context>,
    val supportsSharedSelection: Boolean = true,
) {
    fun forInput(input: KClass<*>?): AnimationDefinition<Context> = inputs[input] ?: default
}

internal class DeclaredItemAnimation<Context>(
    definitions: AnimationDefinitions<Context>,
) : ItemAnimation<Context> {
    var definitions = definitions
        private set
    private val ownedInstances = mutableMapOf<AnimationDefinition<Context>, ItemAnimation<Context>>()
    private var borrowedSource: AnimationDefinitions<Context>? = null
    private var borrowedInstances: MutableMap<AnimationDefinition<Context>, ItemAnimation<Context>>? = null
    private var selectedDefinition: AnimationDefinition<Context>? = null
    private var selectedInstance by mutableStateOf<ItemAnimation<Context>?>(null, referentialEqualityPolicy())
    private var hasContext = false
    private var logicalContext: Context? = null

    init {
        select(definitions.default)
    }

    fun rebind(definitions: AnimationDefinitions<Context>) {
        this.definitions = definitions
        ownedInstances.clear()
        borrowedInstances?.clear()
        borrowedSource = null
        selectedDefinition = null
        select(definitions.default)
    }

    fun definitionFor(input: KClass<*>?): AnimationDefinition<Context> = definitions.forInput(input)

    fun select(
        definition: AnimationDefinition<Context>,
        sourceDefinitions: AnimationDefinitions<Context> = definitions,
    ) {
        val sameSource = if (sourceDefinitions === definitions) borrowedSource == null
            else sourceDefinitions === borrowedSource
        if (definition === selectedDefinition && sameSource) return
        // Shared variants are cached only for the current source catalog.
        val instance = if (sourceDefinitions === definitions) {
            borrowedInstances?.clear()
            borrowedSource = null
            ownedInstances.getOrPut(definition) { definition.createInstance() }
        } else {
            val variants = borrowedInstances ?: mutableMapOf<AnimationDefinition<Context>, ItemAnimation<Context>>()
                .also { borrowedInstances = it }
            if (sourceDefinitions !== borrowedSource) {
                variants.clear()
                borrowedSource = sourceDefinitions
            }
            variants.getOrPut(definition) { definition.createInstance() }
        }
        selectedDefinition = definition
        selectedInstance = instance
        if (hasContext) {
            @Suppress("UNCHECKED_CAST")
            instance.reset(logicalContext as Context)
        }
    }

    private val active: ItemAnimation<Context>
        get() = requireNotNull(selectedInstance)

    override val modifier: Modifier get() = active.modifier
    override fun willBeVisible(context: Context): Boolean = active.willBeVisible(context)

    override fun reset(context: Context) {
        logicalContext = context
        hasContext = true
        active.reset(context)
    }

    override suspend fun animateTo(target: Context) {
        logicalContext = target
        hasContext = true
        active.animateTo(target)
    }

    override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? =
        active.getAndSelectCapability(kClass)
}
