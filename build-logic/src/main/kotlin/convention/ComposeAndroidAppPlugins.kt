package convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class ComposeAndroidAppPlugins  : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        target.pluginManager.apply("org.jetbrains.compose")
    }
}