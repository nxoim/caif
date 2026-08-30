plugins {
    alias(libs.plugins.multiplatform).apply(false)
    alias(libs.plugins.compose).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.android.libraryLegacy).apply(false)
    alias(libs.plugins.publishOnCentral).apply(false)
    id("com.nxoim.gradle.global")
    alias(libs.plugins.dokka) // apply to root
}

dependencies {
    dokka(projects.libraries.core)
    dokka(projects.libraries.decomposeExtensions)
}

tasks.register<Copy>("copyWasmSample") {
    dependsOn("${projects.sample.shared.path}:wasmJsBrowserDistribution")
    from(project(projects.sample.shared.path).layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.layout.projectDirectory.dir("docs/public/other/sample"))
}

tasks.register<Copy>("copyDokkaToPublic") {
    dependsOn(":dokkaGenerateHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    into(rootProject.layout.projectDirectory.dir("docs/public/other/dokka"))
}

tasks.register<Exec>("buildDocs") {
    dependsOn("copyDokkaToPublic", "copyWasmSample")
    workingDir("docs")
    commandLine("npm", "run", "build")
}

tasks.register<Exec>("serveDocs") {
    dependsOn("copyDokkaToPublic", "copyWasmSample")
    workingDir("docs")
    commandLine("npm", "run", "dev")
}

