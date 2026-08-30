package com.nxoim.caif.prefabs.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.nxoim.caif.core.ItemAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.reflect.KClass

@Composable
fun <Key : Any, Context> rememberCollectionSwipeAnimator(
    settleableFactory: (Key) -> ItemAnimation<Context>,
    contextFor: (CollectionItemPosition) -> Context,
    visibleKeys: () -> List<Key>,
    scope: CoroutineScope = rememberCoroutineScope(),
): CollectionSwipeAnimator<Key, Context> {
    val currentSettleableFactory = rememberUpdatedState(settleableFactory)
    val currentContextFor = rememberUpdatedState(contextFor)
    val currentVisibleKeys = rememberUpdatedState(visibleKeys)
    val animator = remember(scope) {
        CollectionSwipeAnimator(
            settleableFactory = { key -> currentSettleableFactory.value(key) },
            contextFor = { position -> currentContextFor.value(position) },
            scope = scope,
            visibleKeys = { currentVisibleKeys.value() },
        )
    }
    DisposableEffect(animator) {
        onDispose { animator.dispose() }
    }
    return animator
}

/**
 * This class only tracks WHICH keys are involved in the active gesture and
 * at WHAT distance from its origin. It does not know about, or dispatch
 * to, capabilities.
 *
 * Calls to this controller and the supplied coroutine scope must be confined
 * to the same dispatcher. The Compose helper provides this confinement by default.
 * [visibleKeys] must return unique keys. Gestures may only start from a returned key.
 */
class CollectionSwipeAnimator<Key : Any, Context>(
    private val settleableFactory: (Key) -> ItemAnimation<Context>,
    private val contextFor: (CollectionItemPosition) -> Context,
    private val scope: CoroutineScope,
    private val visibleKeys: () -> List<Key>,
) {
    private val settleables = mutableStateMapOf<Key, ItemAnimation<Context>>()
    private val lastAnimationTargets = mutableMapOf<Key, CollectionItemPosition>()
    private val animationJobs = mutableMapOf<Key, Job>()
    private var activeGesture: ActiveGesture<Key>? = null

    fun onStart(key: Key) {
        val visibleSnapshot = visibleKeysSnapshot()
        require(key in visibleSnapshot) {
            "A gesture can only start from a visible key."
        }
        val interruptedGesture = activeGesture // capture before overwriting

        activeGesture = ActiveGesture(
            startedKey = key,
            indices = visibleSnapshot.withIndex().associate { it.value to it.index },
            trackedKeys = visibleSnapshot.toMutableList()
        )

        syncVisibleItems(visibleSnapshot, interruptedGesture)
    }

    /**
     * Capability + its distance from the gesture's origin, for every key
     * still tracked by the active gesture. Call this from your own
     * dispatcher to decide what to do with each one — this class never
     * calls capability methods itself.
     */
    fun <T : Any> activeGestureCapabilities(capabilityType: KClass<T>): Map<Key, Pair<T?, Int>> {
        val gesture = activeGesture ?: return emptyMap()
        val startedIndex = gesture.indices[gesture.startedKey] ?: -1

        return buildMap(gesture.trackedKeys.size) {
            gesture.trackedKeys.forEach { key ->
                val distance = distanceFrom(startedIndex, gesture.indices[key] ?: -1)
                val capability = settleables[key]?.getAndSelectCapability(capabilityType)
                put(key, capability to distance)
            }
        }
    }

    /**
     * Call when your capability's own "snap back" logic decides a key
     * should stop tracking the active gesture (equivalent to the old
     * `snapBack` callback in the prototype).
     */
    fun releaseFromGesture(key: Key) {
        activeGesture?.trackedKeys?.remove(key)
        lastAnimationTargets[key]?.let { launchAnimationToTarget(key, it) }
    }

    /**
     * Ends the active gesture. When [preservePositionForKey] is supplied, that key's current
     * animated position is kept so an action surface revealed by the gesture remains interactive;
     * all other affected keys settle normally.
     */
    fun onEnd(preservePositionForKey: Key? = null) {
        val gesture = activeGesture ?: return

        if (preservePositionForKey != null) {
            gesture.trackedKeys.forEach { key ->
                animationJobs.remove(key)?.cancel()
                if (key != preservePositionForKey) {
                    lastAnimationTargets[key]?.let { launchAnimationToTarget(key, it) }
                }
            }
            activeGesture = null
            return
        }

        gesture.trackedKeys.forEach { key ->
            lastAnimationTargets[key]?.let { launchAnimationToTarget(key, it) }
        }

        activeGesture = null
    }

    fun modifierFor(key: Key): Modifier = settleables[key]?.modifier ?: Modifier

    private fun launchAnimationToTarget(
        key: Key,
        position: CollectionItemPosition,
        resolveVisibleIndex: Boolean = true
    ) {
        lastAnimationTargets[key] = position
        val oldJob = animationJobs[key]

        val job = scope.launch(start = CoroutineStart.LAZY) {
            oldJob?.cancelAndJoin()

            val resolvedPosition = if (resolveVisibleIndex) {
                position.copy(index = visibleIndexOf(key))
            } else {
                position
            }
            getOrCreateSettleable(key, resolvedPosition).animateTo(contextFor(resolvedPosition))
        }
        animationJobs[key] = job
        job.start()
    }

    private fun syncVisibleItems(visible: List<Key>, interruptedGesture: ActiveGesture<Key>?) {
        val stillMidDrag = interruptedGesture?.trackedKeys.orEmpty().toSet()
        val newGestureKeys = activeGesture?.trackedKeys.orEmpty().toSet()
        val visibleSet = visible.toSet()

        visible.forEachIndexed { index, key ->
            val isGestureAlreadyOwned = key in stillMidDrag && key in settleables
            if (isGestureAlreadyOwned) return@forEachIndexed
            val previousIndex = lastAnimationTargets[key]?.index
            launchAnimationToTarget(
                key,
                CollectionItemPosition(index = index, previousIndex = previousIndex),
                resolveVisibleIndex = false
            )
        }

        val stale = settleables.keys - visibleSet - newGestureKeys
        stale.forEach { key ->
            animationJobs[key]?.cancel()
            animationJobs -= key
            settleables -= key
            lastAnimationTargets -= key
        }
    }

    private fun getOrCreateSettleable(
        key: Key,
        knownPosition: com.nxoim.caif.prefabs.list.CollectionItemPosition? = null
    ): ItemAnimation<Context> =
        settleables.getOrPut(key) {
            val initialPosition = knownPosition ?: CollectionItemPosition(
                index = visibleIndexOf(key),
                previousIndex = null
            )
            settleableFactory(key).also { settleable ->
                lastAnimationTargets[key] = initialPosition
                settleable.reset(contextFor(initialPosition))
            }
        }

    private fun distanceFrom(startedIndex: Int, index: Int): Int =
        if (startedIndex == -1 || index == -1) Int.MAX_VALUE else abs(index - startedIndex)

    private fun visibleIndexOf(key: Key): Int {
        var visibleIndex = 0
        visibleKeysSnapshot().forEach { candidate ->
            if (candidate == key) return visibleIndex
            visibleIndex++
        }
        return -1
    }

    private fun visibleKeysSnapshot(): List<Key> = visibleKeys().also { keys ->
        require(keys.size == keys.toSet().size) {
            "visibleKeys must contain unique keys."
        }
    }

    internal fun dispose() {
        animationJobs.values.forEach(Job::cancel)
        animationJobs.clear()
        settleables.clear()
        lastAnimationTargets.clear()
        activeGesture = null
    }

    private data class ActiveGesture<Key : Any>(
        val startedKey: Key,
        val indices: Map<Key, Int>,
        val trackedKeys: MutableList<Key>
    )
}

data class CollectionItemPosition(val index: Int, val previousIndex: Int?)

inline fun <reified T : Any, Key : Any> CollectionSwipeAnimator<Key, *>.activeGestureCapabilities(): Map<Key, Pair<T?, Int>> =
    activeGestureCapabilities(T::class)
