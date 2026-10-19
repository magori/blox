plugins {
    alias(libs.plugins.blox.android.library)
    // Les faux dépôts (records, réglages, musiques) et MainDispatcherExtension, partagés par les tests des features.
    alias(libs.plugins.blox.test.fixtures)
}

android {
    namespace = "ch.magori.blox.core.data"
}

dependencies {
    // Les musiques enregistrées gardent des partitions, des gammes et des catégories de core:music.
    api(projects.core.music)
    api(libs.androidx.datastore.core)
    api(libs.androidx.datastore.preferences.core)
    api(libs.kotlinx.coroutines.core)
    // Les DataStore (records, réglages, musiques) sont créés sur un chemin Okio, en production comme dans les tests :
    // le stockage Okio remplace le fichier de façon atomique sur tout système, Windows compris.
    api(libs.okio)
    implementation(libs.androidx.datastore.core.okio)

    // MainDispatcherExtension est une extension JUnit 5 (son type public), qui pose en interne un dispatcher de
    // kotlinx-coroutines-test sur Dispatchers.Main.
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(libs.kotlinx.coroutines.test)

    // Le faux des musiques enregistrées (FakeMusicLibrary) donne des identités de partitions de core:music.
    testFixturesApi(projects.core.music)
}
