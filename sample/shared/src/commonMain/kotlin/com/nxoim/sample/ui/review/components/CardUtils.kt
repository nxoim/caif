package com.nxoim.sample.ui.review.components

import androidx.collection.LruCache
import androidx.collection.mutableOrderedScatterSetOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.nxoim.caif.prefabs.stack.AffectedItemsPolicy
import com.nxoim.caif.prefabs.stack.AppearanceIntention
import com.nxoim.caif.prefabs.stack.ContextResolver
import com.nxoim.caif.prefabs.stack.StackCreationContext
import com.nxoim.evolpagink.compose.PageableComposeState
import com.nxoim.evolpagink.core.PageDisplayingEvent
import com.nxoim.evolpagink.core.Pageable

@Composable
internal fun PageableVisibilityEventsEffect(
    pageableState: PageableComposeState<*>,
    pageable: Pageable<Int, *>,
) {
    LaunchedEffect(pageableState, pageable) {
        snapshotFlow { pageableState.items.value }.collect {
            // The pageable uses zero as its last-page anchor.
            pageable.onVisibilityEvent(PageDisplayingEvent.PageAnchorChanged(0))
        }
    }
}


internal fun <ItemType> defaultCardContextResolver(
    positions: LruCache<String, CardContext.Position>,
    keyFor: (ItemType) -> String,
    environment: () -> CardAnimationEnvironment,
): ContextResolver<ItemType, String, CardContext, StackCreationContext<ItemType>> =
    object : ContextResolver<ItemType, String, CardContext, StackCreationContext<ItemType>> {
        override fun keyFor(itemType: ItemType): String = keyFor(itemType)

        override fun buildContexts(
            stack: List<ItemType>,
            previousStack: List<ItemType>?,
            treatNewEnteringAsPreparing: Boolean,
            recalculateEnteringToMoving: Boolean,
            previousContexts: Map<String, CardContext>?,
        ): Map<String, Pair<CardContext, StackCreationContext<ItemType>>> {
            val previousKeys = previousStack?.mapTo(HashSet()) { keyFor(it) }
            val currentKeys = stack.mapTo(HashSet()) { keyFor(it) }
            val depthByKey = HashMap<String, Int>(stack.size)
            val itemsByKey = LinkedHashMap<String, ItemType>(
                (previousStack?.size ?: 0) + stack.size,
            )
            previousStack?.fastForEach { item -> itemsByKey[keyFor(item)] = item }
            stack.fastForEachIndexed { depth, item ->
                val key = keyFor(item)
                itemsByKey[key] = item
                depthByKey[key] = depth
            }

            return itemsByKey.mapValues { (key, item) ->
                val previousContext = previousContexts?.get(key)
                val isInPrevious = previousKeys?.contains(key) == true
                val isInCurrent = key in currentKeys
                val wasInside = previousContext?.position == CardContext.Position.Inside

                val intention = when {
                    isInPrevious && !isInCurrent -> AppearanceIntention.Removal
                    recalculateEnteringToMoving && previousContext != null && !wasInside ->
                        AppearanceIntention.Movement
                    previousContext == null && !isInPrevious -> AppearanceIntention.Entrance
                    else -> AppearanceIntention.Movement
                }

                val creationContext = StackCreationContext(
                    stackSnapshot = stack,
                    previousSnapshot = previousStack,
                    intention = intention,
                )
                CardContext(
                    depth = depthByKey[key] ?: -1,
                    position = if (intention == AppearanceIntention.Entrance) {
                        CardContext.Position.PreEntered
                    } else {
                        positions[key] ?: CardContext.Position.Inside
                    },
                    environment = environment(),
                ) to creationContext
            }
        }
    }

internal class CardAffectedItemsPolicy<ItemType> :
    AffectedItemsPolicy<ItemType, String, CardContext> {
    override fun selectAffectedItems(
        stack: List<ItemType>,
        keys: Map<ItemType, String>,
        isVisible: (String, CardContext) -> Boolean,
        currentContexts: Map<String, CardContext>,
        targetContexts: Map<String, CardContext>,
        previousContexts: Map<String, CardContext>,
        maxAffected: Int,
        minAffected: Int,
    ): Set<String> {
        val total = mutableOrderedScatterSetOf<String>()
        val newKeys = keys.values.toSet()

        for (index in stack.indices) {
            if (total.size >= maxAffected) break

            val item = stack[index]
            val key = requireNotNull(keys[item]) {
                "Missing key for stack item at index $index"
            }
            val targetContext = requireNotNull(targetContexts[key]) {
                "Missing target context for key $key"
            }
            val willBeVisible = isVisible(key, targetContext)
            val wasVisible = previousContexts[key]
                ?.let { isVisible(key, it) }
                ?: false
            val transitioningToInvisible = wasVisible && !willBeVisible

            if (!willBeVisible && !transitioningToInvisible && total.size >= minAffected) {
                break
            }

            total.add(key)
        }

        for ((key, _) in currentContexts) {
            if (total.size >= maxAffected) break
            if (key !in newKeys) total.add(key)
        }

        return total.asSet()
    }
}

@Composable
internal fun <ItemType> rememberCardContextResolver(
    positions: LruCache<String, CardContext.Position>,
    keyFor: (ItemType) -> String,
    animationEnvironment: State<CardAnimationEnvironment>,
): ContextResolver<ItemType, String, CardContext, StackCreationContext<ItemType>> =
    remember(positions, keyFor, animationEnvironment) {
        defaultCardContextResolver(positions, keyFor) {
            animationEnvironment.value
        }
    }