import convention.setupAndroidMacrobenchmark

plugins {
    alias(libs.plugins.android.libraryLegacy)
    alias(libs.plugins.androidx.benchmark)
}

setupAndroidMacrobenchmark()

dependencies {
    androidTestImplementation(projects.libraries.core)
    androidTestImplementation(projects.libraries.decomposeExtensions)
    androidTestImplementation(libs.androidx.benchmark.junit)
    androidTestImplementation(libs.androidx.benchmark.macro)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
}
