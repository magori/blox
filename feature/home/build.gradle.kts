plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.home"
}

dependencies {
    // La valeur par défaut de `dropUnlessResumed` (LocalLifecycleOwner.current) référence
    // `LifecycleOwner` dans le bytecode de l'écran : il faut donc lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
}

// Dépendances ajoutées quand le code les exigera : core:data en P3, core:audio en P4.
