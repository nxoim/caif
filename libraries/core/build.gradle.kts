import caif.convention.setupPublishing
import convention.setupAndroidTarget
import convention.setupIosTargets
import convention.setupJvmTarget
import convention.setupWebTargets

plugins {
    id("com.nxoim.gradle.compose-multiplatform-plugins")
}

setupPublishing(artifactId = "core", description = "Core caif animation library")

kotlin {
    setupJvmTarget()
    android { setupAndroidTarget(project) }
    setupWebTargets(project, isExecutable = false)
    setupIosTargets()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.animation)
            api(libs.decompose)
            api(libs.androidx.collections)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
