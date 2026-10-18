plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.home"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.audio)
}
