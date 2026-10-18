plugins {
    alias(libs.plugins.blox.android.library)
}

android {
    namespace = "ch.magori.blox.core.audio"
}

dependencies {
    api(projects.core.music)
    implementation(libs.kotlinx.coroutines.android)
}
