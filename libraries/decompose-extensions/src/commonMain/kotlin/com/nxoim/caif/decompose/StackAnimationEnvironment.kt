package com.nxoim.caif.decompose

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.nxoim.caif.prefabs.stack.AppearanceIntention
import com.nxoim.caif.prefabs.stack.ContextFactory
import com.nxoim.caif.prefabs.stack.StackCreationContext
import com.nxoim.caif.prefabs.stack.StackItemPosition
import com.nxoim.caif.prefabs.stack.indexOf
import com.nxoim.caif.prefabs.stack.previousIndexOf

data class StackAnimationEnvironment(
    val viewportSize: Size,
    val layoutDirection: LayoutDirection,
    val density: Density,
)

@Composable
fun BoxWithConstraintsScope.rememberStackAnimationEnvironmentState(): State<StackAnimationEnvironment> {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val maxWidth = constraints.maxWidth.toFloat()
    val maxHeight = constraints.maxHeight.toFloat()
    val state = remember {
        mutableStateOf(
            StackAnimationEnvironment(
                viewportSize = Size(maxWidth, maxHeight),
                layoutDirection = layoutDirection,
                density = density,
            )
        )
    }

    // avoids allocations per frame
    remember(maxWidth, maxHeight, layoutDirection, density) {
        state.value = StackAnimationEnvironment(
            viewportSize = Size(maxWidth, maxHeight),
            layoutDirection = layoutDirection,
            density = density,
        )
    }

    return state
}

data class StackAnimationContext(
    val position: StackItemPosition,
    val environment: StackAnimationEnvironment,
)

fun <ItemType> stackAnimationContextFactory(
    environment: () -> StackAnimationEnvironment
): ContextFactory<ItemType, StackAnimationContext, StackCreationContext<ItemType>> =
    ContextFactory { item ->
        val position = when (intention) {
            AppearanceIntention.Entrance -> StackItemPosition.PreEntered
            AppearanceIntention.Removal -> StackItemPosition.Removed
            AppearanceIntention.Movement -> StackItemPosition.Inside(
                index = indexOf(item),
                previousIndex = previousIndexOf(item),
            )
        }
        StackAnimationContext(
            position = position,
            environment = environment(),
        )
    }
