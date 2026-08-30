import caif.convention.setupPublishing
import convention.setupAndroidTarget
import convention.setupIosTargets
import convention.setupJvmTarget
import convention.setupWebTargets

plugins {
    id("com.nxoim.gradle.compose-multiplatform-plugins")
}

setupPublishing(artifactId = "decompose-extensions", description = "Decompose integration for caif animation library")

kotlin {
    setupJvmTarget()
    android { setupAndroidTarget(project) }
    setupWebTargets(project, isExecutable = false)
    setupIosTargets()

    sourceSets {
        commonMain.dependencies {
            api(projects.libraries.core)
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.animation)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
