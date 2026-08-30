package com.nxoim.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.nxoim.sample.ui.App
import com.nxoim.sample.ui.FakeFlowDependencies
import com.nxoim.sample.ui.FlowComponent

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val component = FlowComponent(
            context = defaultComponentContext(),
            dependencies = FakeFlowDependencies(),
        )

        setContent {
            enableEdgeToEdge()

            App(component)
        }
    }
}
