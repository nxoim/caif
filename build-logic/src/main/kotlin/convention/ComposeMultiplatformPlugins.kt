package convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class ComposeMultiplatformPlugins : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.kotlin.multiplatform.library")
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        target.pluginManager.apply("org.jetbrains.compose")
    }
}