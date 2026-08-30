import convention.setupComposeAndroidApp

plugins {
    id("com.nxoim.gradle.compose-android-app-plugins")
}

android {
    setupComposeAndroidApp()
}

dependencies {
    implementation(libs.decompose)
    implementation(projects.sample.shared)
}
