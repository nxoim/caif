package convention

import org.gradle.api.Plugin
import org.gradle.api.Project

// only needed to make extensions accessible globally
class GlobalPlugin : Plugin<Project> {
    override fun apply(rootProject: Project) {

    }
}