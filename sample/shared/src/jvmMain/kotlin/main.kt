
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.nxoim.sample.ui.App
import com.nxoim.sample.ui.FakeFlowDependencies
import com.nxoim.sample.ui.FlowComponent
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState()
    val lifecycle = remember { LifecycleRegistry() }
    val component = remember {
        lifecycle.resume()
        FlowComponent(
            context = DefaultComponentContext(lifecycle),
            dependencies = FakeFlowDependencies(),
        )
    }

    Window(
        title = "sample",
        state = windowState,
        onCloseRequest = {
            lifecycle.destroy()
            exitApplication()
        },
    ) {
        window.minimumSize = Dimension(350, 600)

        LocalDensity.current.let {
            CompositionLocalProvider(
                LocalDensity provides Density(it.density * 0.8f, it.fontScale)
            ) {
                App(component)

            }
        }
    }
}
