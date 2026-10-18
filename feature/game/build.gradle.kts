plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.game"
}

// Dépendances ajoutées quand le code les exigera : core:game en P2, core:data en P3,
// core:audio en P4.
