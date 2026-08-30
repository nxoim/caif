package com.nxoim.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nxoim.sample.ui.common.sharedtransition.LocalSharedElementsEnabled
import com.nxoim.sample.ui.theme.SampleTheme

@Composable
fun App(component: FlowComponent) {
    var sharedElementsEnabled by remember { mutableStateOf(true) }

    CompositionLocalProvider(
        LocalSharedElementsEnabled provides sharedElementsEnabled,
    ) {
        SampleTheme {
            FlowDemo(
                component = component,
                sharedElementsEnabled = sharedElementsEnabled,
                onToggleSharedElements = { sharedElementsEnabled = it },
            )
        }
    }
}
