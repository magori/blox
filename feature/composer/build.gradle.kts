plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.composer"
}

dependencies {
    // Le modèle de l'éditeur lit et écrit des partitions (Score, Voice, Note, Pitch, Scale, Category).
    implementation(projects.core.music)
    // Les règles des noms des musiques enregistrées (MusicNames de core:data), une seule source avec la bibliothèque.
    implementation(projects.core.data)
    // `api` : ComposerViewModel est un ViewModel, et app construit sa fabrique.
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    // `collectAsStateWithLifecycle` lit le `LifecycleOwner` courant : il faut lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    // `@StringRes` sur les libellés des onglets, des modes et des noms de notes.
    implementation(libs.androidx.annotation)

    // Le contraste de WCAG et les palettes nommées, partagés avec les autres features.
    testImplementation(testFixtures(projects.core.designsystem))
}
