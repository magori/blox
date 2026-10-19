plugins {
    alias(libs.plugins.blox.android.library)
    // Le faux lecteur de musique (FakeMusicPlayer), partagé par les tests des features qui jouent de la musique.
    alias(libs.plugins.blox.test.fixtures)
}

android {
    namespace = "ch.magori.blox.core.audio"
}

dependencies {
    // L'API publique reçoit des partitions de core:music, et le séquenceur une portée et des
    // dispatchers de coroutines.
    api(projects.core.music)
    api(libs.kotlinx.coroutines.core)

    // Le faux lecteur reçoit et note des partitions de core:music.
    testFixturesApi(projects.core.music)
}
