plugins {
    alias(libs.plugins.blox.android.library)
}

android {
    namespace = "ch.magori.blox.core.data"
}

// Dépendances ajoutées quand le code les exigera (P3.1) : DataStore Preferences et coroutines.
