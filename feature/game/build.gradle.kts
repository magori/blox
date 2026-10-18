plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.game"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit des types du domaine, et
    // GameViewModel est un ViewModel.
    api(projects.core.game)
    api(libs.androidx.lifecycle.viewmodel)

    // Le seul module de feature qui a un ViewModel, pour l'instant.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.core)
}

// Dépendances ajoutées quand le code les exigera : core:data en P3, core:audio en P4.
