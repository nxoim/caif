package com.nxoim.caif.stack

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.nxoim.caif.core.AnimationSelectionStrategy
import com.nxoim.caif.core.ItemAnimation
import com.nxoim.caif.core.ItemAnimationFactory
import com.nxoim.caif.core.buildAnimationFactory
import com.nxoim.caif.core.chooseStrategy
import com.nxoim.caif.core.fromItem
import com.nxoim.caif.core.perItem
import com.nxoim.caif.prefabs.stack.AppearanceIntention
import com.nxoim.caif.prefabs.stack.ItemAnimationRegistry
import com.nxoim.caif.prefabs.stack.RenderOrderStrategy
import com.nxoim.caif.prefabs.stack.StackCreationContext
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.StackOrchestrator
import com.nxoim.caif.prefabs.stack.defaultStackContextResolver
import com.nxoim.caif.prefabs.stack.indexOf
import com.nxoim.caif.prefabs.stack.previousIndexOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class AnimationFactorySelectionTest {
    @Test
    fun givenFactoryDecorator_whenSelectingAndReplacing_thenDeclaredInputsArePreserved() = runTest {
        for (strategy in listOf(perItem<String>(), fromItem { it.last() })) {
            fun factory(label: String): ItemAnimationFactory<String, String, StackItemPosition> {
                val built = buildAnimationFactory<String, String, StackItemPosition>(strategy) { item, _ ->
                    defaultAnimation { RecordingAnimation("default") }
                    onInput<AlternateInput> { AlternateAnimation("$label-$item") }
                }
                return object : ItemAnimationFactory<String, String, StackItemPosition> by built {}
            }
            val stack = mutableStateOf(listOf("base", "details"))
            var current = factory("old")
            val orchestrator = orchestrator(stack, registry = ItemAnimationRegistry.fromProvider { current })
            runCurrent()
            val old = orchestrator.startCycle(AlternateInput::class).getValue("base")!!
            assertEquals(if (strategy.sourceItem(null, stack.value) == null) "old-base" else "old-details", old.source)
            current = factory("new")
            val next = orchestrator.startCycle(AlternateInput::class).getValue("base")!!
            assertEquals(if (strategy.sourceItem(null, stack.value) == null) "new-base" else "new-details", next.source)
            assertNotSame(old, next)
        }
    }

    @Test
    fun givenFactoryReplacement_whenInputSettles_thenReplacementWaitsForNextCycle() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        fun factory(label: String) = buildAnimationFactory<String, String, StackItemPosition>(fromItem { it.last() }) { _, _ ->
            defaultAnimation(onInput<DragInput> { RecordingAnimation(label) })
        }
        var current = factory("old")
        var reads = 0
        val registry = ItemAnimationRegistry.fromProvider { reads++; current }
        val orchestrator = orchestrator(stack, registry = registry)
        runCurrent()
        val slot = registry.animations.getValue("base")
        val before = reads
        val old = orchestrator.startCycle(DragInput::class).getValue("base")!!
        assertEquals(before + 1, reads)
        current = factory("new")
        old.drag(5f)
        orchestrator.progressCycle(orchestrator.currentCycleId)
        runCurrent()
        assertSame(old, slot.getAndSelectCapability(DragInput::class))
        assertEquals(before + 1, reads)

        val next = orchestrator.startCycle(DragInput::class).getValue("base")!!
        assertEquals("new", next.source)
        assertNotSame(old, next)
        assertSame(slot, registry.animations.getValue("base"))
        assertSame(next, orchestrator.startCycle(DragInput::class).getValue("base"))
    }

    @Test
    fun givenReplacementDuringSourceSelection_whenCycleCreatesItems_thenFactoryIsCapturedOnce() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val replacement = buildAnimationFactory<String, String, StackItemPosition> { _, _ ->
            defaultAnimation { RecordingAnimation("new") }
        }
        lateinit var current: ItemAnimationFactory<String, String, StackItemPosition>
        current = buildAnimationFactory(selectionStrategy = AnimationSelectionStrategy { _, items ->
            current = replacement
            items.last()
        }) { _: String, _: String -> defaultAnimation { RecordingAnimation("old") } }
        val orchestrator = orchestrator(stack, registry = ItemAnimationRegistry.fromProvider { current })
        runCurrent()
        assertEquals(setOf("old"), orchestrator.itemsToRender.map {
            it.second.getAndSelectCapability(DragInput::class)!!.source
        }.toSet())
        assertEquals(setOf("new"), orchestrator.startCycle(DragInput::class).values.map { it!!.source }.toSet())
    }

    @Test
    fun givenVisibilityShrinks_whenFactoryChanges_thenObsoleteRenderEntryWaitsForExternalAnimation() = runTest {
        for ((externalRunning, registeredBeforeSettle) in listOf(false to false, true to false, true to true)) {
            val stack = mutableStateOf(listOf("base", "middle", "top"))
            fun factory(depth: Int) = buildAnimationFactory<String, String, StackItemPosition> { _, _ ->
                defaultAnimation { DepthAnimation("depth-$depth", depth) }
            }
            var current = factory(2)
            val registry = ItemAnimationRegistry.fromProvider { current }
            val orchestrator = orchestrator(stack, registry = registry)
            val running = mutableStateOf(externalRunning)
            val earlyRegistration = if (registeredBeforeSettle)
                orchestrator.registerExternalAnimation("base") { running.value }
            else null
            runCurrent()
            assertEquals(3, orchestrator.itemsToRender.size)
            val registration = earlyRegistration ?: orchestrator.registerExternalAnimation("base") { running.value }
            current = factory(0)
            orchestrator.startCycle(stack.value)
            orchestrator.progressCycle()
            runCurrent()
            assertEquals(if (externalRunning) listOf("base", "top") else listOf("top"),
                orchestrator.itemsToRender.map { it.first.first })
            Snapshot.withMutableSnapshot { running.value = false }
            runCurrent()
            assertEquals(listOf("top"), orchestrator.itemsToRender.map { it.first.first })
            assertEquals(3, registry.animations.size)
            current = factory(2)
            orchestrator.startCycle(stack.value)
            orchestrator.progressCycle()
            runCurrent()
            assertEquals(3, orchestrator.itemsToRender.size)
            registration.unregister()
        }
    }

    @Test
    fun givenEqualInstances_whenDefinitionChanges_thenSelectedInstanceIsReplacedByIdentity() = runTest {
        val stack = mutableStateOf(listOf("item"))
        lateinit var drag: EqualAnimation
        lateinit var alternate: EqualAnimation
        val factory = buildAnimationFactory<String, String, StackItemPosition> { _, _ ->
            val initial = onInput<DragInput> { EqualAnimation("style").also { drag = it } }
            onInput<AlternateInput> { EqualAnimation("style").also { alternate = it } }
            defaultAnimation(initial)
        }
        val orchestrator = orchestrator(stack, factory = factory)
        runCurrent()
        advanceUntilIdle()
        orchestrator.startCycle(DragInput::class).getValue("item")!!.drag(5f)

        val selected = orchestrator.startCycle(AlternateInput::class).getValue("item")
        assertEquals(drag, alternate)
        assertNotSame(drag, alternate)
        assertSame(alternate, selected)
        assertEquals(0f, alternate.offset)
        assertEquals(5f, drag.offset)

        val repeated = orchestrator.startCycle(AlternateInput::class).getValue("item")
        assertSame(alternate, repeated)
    }

    @Test
    fun givenDefaultStrategy_whenCycleStarts_thenEachItemUsesItsOwnDeclarations() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val orchestrator = orchestrator(stack)
        runCurrent()
        advanceUntilIdle()

        val capabilities = orchestrator.startCycle(DragInput::class)
        assertEquals("base", capabilities.getValue("base")?.source)
        assertEquals("details", capabilities.getValue("details")?.source)
    }

    @Test
    fun givenCommonSource_whenInputSelected_thenItemsUseIndependentInstancesOfSourceDefinition() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val orchestrator = orchestrator(stack, fromItem { it.last() })
        runCurrent()
        advanceUntilIdle()

        val capabilities = orchestrator.startCycle(DragInput::class)
        val underlying = capabilities.getValue("base")!!
        val top = capabilities.getValue("details")!!
        assertEquals("details", underlying.source)
        assertEquals("details", top.source)
        assertNotSame(underlying, top)

        underlying.drag(5f)
        assertEquals(5f, underlying.offset)
        assertEquals(0f, top.offset)
    }

    @Test
    fun givenCommonSource_whenInputChanges_thenUnderlyingItemKeepsTheSelectedSource() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val orchestrator = orchestrator(stack, fromItem { it.last() })
        runCurrent()
        advanceUntilIdle()

        val capabilities = orchestrator.startCycle(AlternateInput::class)
        assertEquals("details-alternate", capabilities.getValue("base")?.source)
        assertEquals("details-alternate", capabilities.getValue("details")?.source)
    }

    @Test
    fun givenCommonSource_whenInputVariantIsReselected_thenBothItemsReuseTheirInstances() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val orchestrator = orchestrator(stack, fromItem { it.last() })
        runCurrent()
        advanceUntilIdle()
        val first = orchestrator.startCycle(DragInput::class)
        orchestrator.startCycle(AlternateInput::class)

        val repeated = orchestrator.startCycle(DragInput::class)
        assertSame(first.getValue("base"), repeated.getValue("base"))
        assertSame(first.getValue("details"), repeated.getValue("details"))
    }

    @Test
    fun givenUnmappedInput_whenCycleStarts_thenSourceDefaultIsSelected() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val orchestrator = orchestrator(stack, fromItem { it.last() })
        runCurrent()
        advanceUntilIdle()
        orchestrator.startCycle(AlternateInput::class)

        val capabilities = orchestrator.startCycle(UnmappedInput::class)
        assertEquals(setOf(null), capabilities.values.toSet())
        assertEquals(setOf("details"), orchestrator.itemsToRender.map {
            it.second.getAndSelectCapability(DragInput::class)!!.source
        }.toSet())
    }

    @Test
    fun givenInputSpecificVisibility_whenSharedDefinitionChanges_thenParticipationUsesSelectedDefinition() = runTest {
        val stack = mutableStateOf(listOf("base", "middle", "details"))
        val factory = buildAnimationFactory<String, String, StackItemPosition>(fromItem { it.last() }) { item, _ ->
            onInput<DragInput> { DepthAnimation(item, 2) }
            onInput<AlternateInput> { AlternateDepthAnimation(item, 0) }
            defaultAnimation { DepthAnimation(item, 0) }
        }
        val orchestrator = orchestrator(stack, factory = factory)
        runCurrent()
        advanceUntilIdle()
        assertEquals(setOf("details"), orchestrator.itemsToRender.map { it.first.first }.toSet())

        assertEquals(setOf("base", "middle", "details"), orchestrator.startCycle(DragInput::class).keys)
        assertEquals(setOf("middle", "details"), orchestrator.startCycle(AlternateInput::class).keys)
    }

    @Test
    fun givenCommonSource_whenTargetTopChanges_thenDefaultSelectionFollowsTarget() = runTest {
        val stack = mutableStateOf(listOf("base"))
        val orchestrator = orchestrator(stack, fromItem { it.last() })
        runCurrent()
        advanceUntilIdle()
        val original = orchestrator.itemsToRender.single().second.getAndSelectCapability(DragInput::class)

        Snapshot.withMutableSnapshot { stack.value = listOf("base", "details") }
        runCurrent()
        advanceUntilIdle()
        assertEquals(
            setOf("details"),
            orchestrator.itemsToRender.map { it.second.getAndSelectCapability(DragInput::class)!!.source }.toSet(),
        )

        Snapshot.withMutableSnapshot { stack.value = listOf("base") }
        runCurrent()
        advanceUntilIdle()
        assertSame(original, orchestrator.itemsToRender.single().second.getAndSelectCapability(DragInput::class))

        Snapshot.withMutableSnapshot { stack.value = emptyList() }
        runCurrent()
        advanceUntilIdle()
        assertEquals(emptyList(), orchestrator.itemsToRender)
    }

    @Test
    fun givenInputCycle_whenConfirmedOrCancelled_thenSelectionPersistsThroughSettling() = runTest {
        for (confirm in listOf(false, true)) {
            val stack = mutableStateOf(listOf("base", "details"))
            val orchestrator = orchestrator(stack, fromItem { it.last() })
            runCurrent()
            advanceUntilIdle()
            val capabilities = orchestrator.startCycle(DragInput::class)
            val underlying = capabilities.getValue("base")!!
            underlying.drag(5f)
            if (confirm) Snapshot.withMutableSnapshot { stack.value = listOf("base") }

            orchestrator.progressCycle(orchestrator.currentCycleId)
            runCurrent()
            advanceUntilIdle()

            val baseAnimation = orchestrator.itemsToRender.first { it.first.first == "base" }.second
            assertSame(underlying, baseAnimation.getAndSelectCapability(DragInput::class))
            assertEquals("details", underlying.source)
            assertEquals(0f, underlying.offset)
        }
    }

    @Test
    fun givenConditionalStrategy_whenInputChanges_thenChoiceUsesExistingSnapshotOncePerCycle() = runTest {
        val stack = mutableStateOf(listOf("base", "details"))
        val shared = fromItem<String> { it.last() }
        val individual = perItem<String>()
        var selections = 0
        var share = false
        val strategy = chooseStrategy<String> { input, snapshot ->
            assertSame(stack.value, snapshot)
            selections++
            if (share && input == DragInput::class) shared else individual
        }
        val orchestrator = orchestrator(stack, strategy)
        runCurrent()
        advanceUntilIdle()
        val initialSelections = selections

        share = true
        val capabilities = orchestrator.startCycle(DragInput::class)
        assertEquals(initialSelections + 1, selections)
        assertEquals("details", capabilities.getValue("base")?.source)
        capabilities.values.forEach { it?.drag(1f) }
        assertEquals(initialSelections + 1, selections)

        share = false
        val next = orchestrator.startCycle(DragInput::class)
        assertEquals("base", next.getValue("base")?.source)
    }

    @Test
    fun givenInvalidSource_whenCycleStarts_thenSelectionFailsClearly() = runTest {
        val stack = mutableStateOf(listOf("base"))
        val missing = fromItem<String> { "missing" }
        val individual = perItem<String>()
        val orchestrator = orchestrator(stack, chooseStrategy { input, _ ->
            if (input == DragInput::class) missing else individual
        })
        runCurrent()
        advanceUntilIdle()
        assertFailsWith<IllegalArgumentException> {
            orchestrator.startCycle(DragInput::class)
        }
    }

    private fun kotlinx.coroutines.test.TestScope.orchestrator(
        stack: androidx.compose.runtime.State<List<String>>,
        strategy: AnimationSelectionStrategy<String> = perItem(),
        factory: ItemAnimationFactory<String, String, StackItemPosition> =
            buildAnimationFactory(strategy) { item, _ ->
                val drag = onInput<DragInput> { RecordingAnimation(item) }
                onInput<AlternateInput> { AlternateAnimation("$item-alternate") }
                defaultAnimation(drag)
            },
        registry: ItemAnimationRegistry<String, String, StackItemPosition> = ItemAnimationRegistry(factory),
    ): StackOrchestrator<String, String, StackItemPosition, StackCreationContext<String>> {
        return StackOrchestrator(
            scope = backgroundScope,
            stack = stack,
            registry = registry,
            resolver = defaultStackContextResolver(
                contextFactory = { item ->
                    when (intention) {
                        AppearanceIntention.Entrance -> StackItemPosition.PreEntered
                        AppearanceIntention.Removal -> StackItemPosition.Removed
                        AppearanceIntention.Movement -> StackItemPosition.Inside(
                            stackSnapshot.lastIndex - indexOf(item),
                            previousSnapshot?.let { old -> previousIndexOf(item)?.let { old.lastIndex - it } },
                        )
                    }
                },
                keyFor = { it },
            ),
            maxAffected = Int.MAX_VALUE,
            renderOrder = RenderOrderStrategy.byStackIndex(),
        )
    }
}

private interface DragInput {
    val source: String
    val offset: Float
    fun drag(delta: Float)
}

private interface AlternateInput {
    val source: String
}

private interface UnmappedInput

private open class RecordingAnimation(override val source: String) : ItemAnimation<StackItemPosition>, DragInput {
    override val modifier = Modifier
    override var offset = 0f
    override fun drag(delta: Float) { offset += delta }
    override fun reset(context: StackItemPosition) { offset = 0f }
    override suspend fun animateTo(target: StackItemPosition) { offset = 0f }
    override fun willBeVisible(context: StackItemPosition): Boolean = context is StackItemPosition.Inside
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAndSelectCapability(kClass: KClass<T>): T? =
        if (kClass.isInstance(this)) this as T else null
}

private class AlternateAnimation(source: String) : RecordingAnimation(source), AlternateInput

private open class DepthAnimation(source: String, private val visibleDepth: Int) : RecordingAnimation(source) {
    override fun willBeVisible(context: StackItemPosition): Boolean =
        context is StackItemPosition.Inside && context.index <= visibleDepth
}

private class AlternateDepthAnimation(source: String, visibleDepth: Int) :
    DepthAnimation(source, visibleDepth), AlternateInput

// Equality deliberately excludes the inherited mutable animation state.
private data class EqualAnimation(val configuration: String) : RecordingAnimation(configuration), AlternateInput
