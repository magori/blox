plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.musics"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit la bibliothèque des musiques de core:data et le
    // lecteur de musique de core:audio (écoute depuis la liste) ; MyMusicsViewModel est un ViewModel.
    api(projects.core.audio)
    api(projects.core.data)
    // Les partitions écoutées (Score) et les catégories des musiques (Category).
    implementation(projects.core.music)
    api(libs.androidx.lifecycle.viewmodel)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // `collectAsStateWithLifecycle` et `dropUnlessResumed` lisent le `LifecycleOwner` courant : il faut
    // lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.kotlinx.coroutines.core)
    // `@StringRes` sur les noms des catégories.
    implementation(libs.androidx.annotation)

    // Le faux lecteur de musique, partagé avec les autres features.
    testImplementation(testFixtures(projects.core.audio))
    // La fausse bibliothèque des musiques et MainDispatcherExtension, partagées avec les autres features.
    testImplementation(testFixtures(projects.core.data))
}
