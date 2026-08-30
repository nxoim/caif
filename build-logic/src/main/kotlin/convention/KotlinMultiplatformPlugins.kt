package convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinMultiplatformPlugins : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.kotlin.multiplatform.library")
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    }
}
