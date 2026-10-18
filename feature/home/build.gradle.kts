plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.home"
}

// Dépendances ajoutées quand le code les exigera : core:data en P3, core:audio en P4.
