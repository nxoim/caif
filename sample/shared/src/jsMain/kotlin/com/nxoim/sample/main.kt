package com.nxoim.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.nxoim.sample.ui.App
import com.nxoim.sample.ui.FakeFlowDependencies
import com.nxoim.sample.ui.FlowComponent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    lifecycle.resume()
    val component = FlowComponent(
        context = DefaultComponentContext(lifecycle),
        dependencies = FakeFlowDependencies(),
    )

    ComposeViewport {
        Box(Modifier.fillMaxSize()) {
            App(component)
        }
    }
}
