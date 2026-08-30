import convention.setupAndroidTarget
import convention.setupIosTargets
import convention.setupJvmTarget
import convention.setupWebTargets
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("com.nxoim.gradle.compose-multiplatform-plugins")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    setupJvmTarget()
    android { setupAndroidTarget(project) }
    setupWebTargets(project, isExecutable = true, moduleName = "composeApp")
    setupIosTargets(baseFrameworkName = "ComposeApp")

    sourceSets {
        commonMain.dependencies {
            api(projects.libraries.core)
            api(projects.libraries.decomposeExtensions)
            api(libs.compose.runtime)
            api(libs.compose.material3)
            api(libs.compose.material3Adaptive)
            api(libs.compose.materialIconsExtended)
            api(libs.kotlinx.serialization.json)
            api(libs.compose.preview)
            implementation(libs.evolpagink)
            implementation(libs.evolpaginkCore)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            api(libs.androidx.appcompat)
            api(libs.androidx.activityCompose)
            api(libs.compose.uitooling)
            api(libs.kotlinx.coroutines.android)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.animationGraphics)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "com.nxoim.sample.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}
