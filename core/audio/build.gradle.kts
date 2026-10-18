plugins {
    alias(libs.plugins.blox.android.library)
}

android {
    namespace = "ch.magori.blox.core.audio"
}

dependencies {
    // L'API publique du synthétiseur reçoit des voix de core:music.
    api(projects.core.music)
}

// Dépendance ajoutée quand le code l'exigera (P4.4) : coroutines pour le séquenceur.
