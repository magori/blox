plugins {
    alias(libs.plugins.blox.android.library)
}

android {
    namespace = "ch.magori.blox.core.data"
}

dependencies {
    api(libs.androidx.datastore.core)
    api(libs.androidx.datastore.preferences.core)
    api(libs.kotlinx.coroutines.core)
    // Le DataStore des records est créé sur un chemin Okio, en production comme dans les tests :
    // le stockage Okio remplace le fichier de façon atomique sur tout système, Windows compris.
    api(libs.okio)
    implementation(libs.androidx.datastore.core.okio)
}
