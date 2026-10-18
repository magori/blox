plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.game"
}

dependencies {
    implementation(projects.core.game)
    implementation(projects.core.data)
    implementation(projects.core.audio)
    implementation(libs.kotlinx.coroutines.android)
}
