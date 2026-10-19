plugins {
    alias(libs.plugins.blox.android.library)
    // Les faux dépôts (records, réglages) et MainDispatcherExtension, partagés par les tests des features.
    alias(libs.plugins.blox.test.fixtures)
}

android {
    namespace = "ch.magori.blox.core.data"
}

dependencies {
    api(libs.androidx.datastore.core)
    api(libs.androidx.datastore.preferences.core)
    api(libs.kotlinx.coroutines.core)
    // Les DataStore (records et réglages) sont créés sur un chemin Okio, en production comme dans les tests :
    // le stockage Okio remplace le fichier de façon atomique sur tout système, Windows compris.
    api(libs.okio)
    implementation(libs.androidx.datastore.core.okio)

    // MainDispatcherExtension est une extension JUnit 5 (son type public), qui pose en interne un dispatcher de
    // kotlinx-coroutines-test sur Dispatchers.Main.
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
}
