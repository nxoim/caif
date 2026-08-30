package com.nxoim.sample

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.nxoim.sample.ui.App
import com.nxoim.sample.ui.FakeFlowDependencies
import com.nxoim.sample.ui.FlowComponent

fun MainViewController() = run {
    val lifecycle = LifecycleRegistry()
    lifecycle.resume()
    val component = FlowComponent(
        context = DefaultComponentContext(lifecycle),
        dependencies = FakeFlowDependencies(),
    )

    ComposeUIViewController {
        App(component)
    }
}
