plugins {
    `kotlin-dsl`
    `java-library`
    alias(libs.plugins.dokka)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.publishOnCentralPlugin)
}

gradlePlugin {
    plugins {
        register("global") {
            id = "com.nxoim.gradle.global"
            implementationClass = "convention.GlobalPlugin"
        }

        // applied internally by setupPublishing()
        register("publish") {
            id = "com.nxoim.gradle.publish"
            implementationClass = "convention.PublishPlugin"
        }
        register("compose-multiplatform-plugins") {
            id = "com.nxoim.gradle.compose-multiplatform-plugins"
            implementationClass = "convention.ComposeMultiplatformPlugins"
        }

        register("kotlin-multiplatform-plugins") {
            id = "com.nxoim.gradle.kotlin-multiplatform-plugins"
            implementationClass = "convention.KotlinMultiplatformPlugins"
        }

        register("compose-android-app-plugins") {
            id = "com.nxoim.gradle.compose-android-app-plugins"
            implementationClass = "convention.ComposeAndroidAppPlugins"
        }
    }
}
