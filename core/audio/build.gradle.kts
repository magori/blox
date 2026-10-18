plugins {
    alias(libs.plugins.blox.android.library)
}

android {
    namespace = "ch.magori.blox.core.audio"
}

// Dépendances ajoutées quand le code les exigera (P4) : api(projects.core.music) pour exposer
// les partitions, coroutines pour le séquenceur.
