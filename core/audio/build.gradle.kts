plugins {
    alias(libs.plugins.blox.android.library)
}

android {
    namespace = "ch.magori.blox.core.audio"
}

dependencies {
    // L'API publique reçoit des partitions de core:music, et le séquenceur une portée et des
    // dispatchers de coroutines.
    api(projects.core.music)
    api(libs.kotlinx.coroutines.core)
}
