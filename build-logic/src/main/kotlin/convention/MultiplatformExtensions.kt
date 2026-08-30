package convention

import caif.convention.AndroidBuildStuff
import caif.convention.Constants
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

fun KotlinMultiplatformExtension.setupJvmTarget(): KotlinJvmTarget = jvm()


fun KotlinMultiplatformAndroidLibraryTarget.setupAndroidTarget(
    project: Project,
    namespace: String = "${Constants.packageRootName}.${project.name.replace('-', '.')}"
) {
    this.namespace = namespace

    compileSdk { version = release(AndroidBuildStuff.compileSdk) }
    minSdk { version = release(AndroidBuildStuff.minSdk) }

    androidResources {
        this.enable = true
    }

    withHostTest {}

    this.optimization {
        this.consumerKeepRules.files.add(File("consumer-proguard-rules.pro"))
    }
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.setupWebTargets(
    project: Project,
    isExecutable: Boolean = false,
    moduleName: String = project.name
) {
    wasmJs("wasmJs") {
        outputModuleName.set(project.provider { moduleName })

        browser {
            commonWebpackConfig { outputFileName = "$moduleName.js" }
        }

        binaries.executable()
    }

    js("js") {
        outputModuleName.set(project.provider { moduleName })

        browser {
            commonWebpackConfig { outputFileName = "$moduleName.js" }
        }

        binaries.executable()
    }
}

fun KotlinMultiplatformExtension.setupIosTargets(
    baseFrameworkName: String? = null
) {
    val targets = listOf(iosArm64(), iosSimulatorArm64())

    if (baseFrameworkName != null) {
        targets.forEach { target ->
            target.binaries.framework {
                baseName = baseFrameworkName
                isStatic = true
            }
        }
    }
}

fun KotlinMultiplatformExtension.setupTvosTargets(
    baseFrameworkName: String? = null
) {
    val targets = listOf(tvosArm64(), tvosSimulatorArm64())

    if (baseFrameworkName != null) {
        targets.forEach { target ->
            target.binaries.framework {
                baseName = baseFrameworkName
                isStatic = true
            }
        }
    }
}

fun KotlinMultiplatformExtension.setupWatchosTargets(
    baseFrameworkName: String? = null
) {
    val targets = listOf(
        watchosArm32(),
        watchosArm64(),
        watchosDeviceArm64(),
        watchosSimulatorArm64()
    )

    if (baseFrameworkName != null) {
        targets.forEach { target ->
            target.binaries.framework {
                baseName = baseFrameworkName
                isStatic = true
            }
        }
    }
}
