plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.home"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit les dépôts de core:data et la
    // progression des niveaux de core:game ; HomeViewModel est un ViewModel.
    api(projects.core.data)
    api(projects.core.game)
    api(libs.androidx.lifecycle.viewmodel)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // La valeur par défaut de `dropUnlessResumed` (LocalLifecycleOwner.current) référence
    // `LifecycleOwner` dans le bytecode de l'écran : il faut donc lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.kotlinx.coroutines.core)
    // `@StringRes` sur les libellés des choix.
    implementation(libs.androidx.annotation)
}

// Dépendance ajoutée quand le code l'exigera : core:audio en P4.
