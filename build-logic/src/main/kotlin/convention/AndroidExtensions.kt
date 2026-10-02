package convention

import caif.convention.AndroidBuildStuff
import caif.convention.Constants
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

fun ApplicationExtension.setupComposeAndroidApp(
    namespace: String = "${Constants.packageRootName}.sample"
) {
    this.namespace = namespace

    compileSdk { version = release(AndroidBuildStuff.compileSdk) }

    defaultConfig {
        minSdk { version = release(AndroidBuildStuff.minSdk) }
        targetSdk { version = release(AndroidBuildStuff.compileSdk) }
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        val debugSigningConfig = signingConfigs.getByName("debug")

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = debugSigningConfig

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }
}

fun Project.setupAndroidMacrobenchmark(
    namespace: String = "com.nxoim.caif.${project.name}"
) {
    extensions.findByType(LibraryExtension::class.java)?.apply {
        this.namespace = namespace

        compileSdk { version = release(AndroidBuildStuff.compileSdk) }

        defaultConfig {
            minSdk { version = release(AndroidBuildStuff.minSdk) }

            testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
            testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"] =
                "MethodTracing"
            testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
                "EMULATOR,LOW-BATTERY"
        }
        testBuildType = "release"
    }
}