plugins {
    alias(libs.plugins.blox.android.benchmark)
}

android {
    namespace = "ch.magori.blox.benchmark"
    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.test.monitor)
    implementation(libs.opentest4j)
}
