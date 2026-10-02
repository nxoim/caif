package com.nxoim.caif.core

import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SelectableItemAnimationBuilderTest {
    private interface InitialCapability
    private interface AddedCapability

    private class InitialAnimation : BaseItemAnimation<String>(), InitialCapability {
        override val modifier = Modifier
    }

    private class AddedAnimation : BaseItemAnimation<String>(), AddedCapability {
        override val modifier = Modifier
    }

    @Test
    fun givenBuiltSelectableAnimation_whenCapturedBuilderIsExtended_thenBuiltCapabilitiesStayUnchanged() {
        lateinit var capturedBuilder: SelectableItemAnimationBuilder<String>
        val animation = buildSelectableItemAnimation {
            capturedBuilder = this
            selectOnCapability<InitialCapability> { InitialAnimation() }
        }

        assertNotNull(animation.getAndSelectCapability<InitialCapability>())
        assertNull(animation.getAndSelectCapability<AddedCapability>())

        capturedBuilder.selectOnCapability<AddedCapability> { AddedAnimation() }

        assertNull(animation.getAndSelectCapability<AddedCapability>())
        assertNotNull(animation.getAndSelectCapability<InitialCapability>())
    }
}
