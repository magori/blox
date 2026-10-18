plugins {
    alias(libs.plugins.blox.android.library)
    alias(libs.plugins.blox.android.library.compose)
}

android {
    namespace = "ch.magori.blox.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
}
