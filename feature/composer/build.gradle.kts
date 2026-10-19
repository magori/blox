plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.composer"
}

dependencies {
    // Le modèle de l'éditeur lit et écrit des partitions (Score, Voice, Note, Pitch, Scale, Category).
    // `api` : la fabrique reçoit un `MusicPlayer`, alias de `Player<Music>` : `Music` est dans sa signature.
    api(projects.core.music)
    // Les règles des noms des musiques enregistrées (MusicNames de core:data), une seule source avec la bibliothèque.
    implementation(projects.core.data)
    // `api` : la fabrique du ViewModel, construite par app, reçoit le lecteur de musique de core:audio (« Écouter »).
    api(projects.core.audio)
    // `api` : ComposerViewModel est un ViewModel, et app construit sa fabrique.
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    // `collectAsStateWithLifecycle` lit le `LifecycleOwner` courant : il faut lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    // `@StringRes` sur les libellés des onglets, des modes et des noms de notes.
    implementation(libs.androidx.annotation)

    // Le faux lecteur de musique, partagé avec les autres features.
    testImplementation(testFixtures(projects.core.audio))
    // MainDispatcherExtension, partagée avec les autres features.
    testImplementation(testFixtures(projects.core.data))
    // Le contraste de WCAG et les palettes nommées, partagés avec les autres features.
    testImplementation(testFixtures(projects.core.designsystem))
    // Le ViewModel de l'écran y est construit avec le faux lecteur des tests unitaires.
    androidTestImplementation(testFixtures(projects.core.audio))
}
