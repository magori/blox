plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
    // Le reflet et la couleur des notes, jetons melody-hi, melody, bass-hi et bass de docs/design-system/tokens.json,
    // comparés aux bulles des notes par TrackColorsTest (ADR 0019).
    alias(libs.plugins.blox.design.tokens)
}

android {
    namespace = "ch.magori.blox.feature.composer"
}

dependencies {
    // Le modèle de l'éditeur lit et écrit des partitions (Score, Voice, Note, Pitch, Scale, Category).
    // `api` : la fabrique reçoit un `MusicPlayer`, alias de `Player<Music>` : `Music` est dans sa signature.
    api(projects.core.music)
    // `api` : la fabrique reçoit les musiques enregistrées (MusicLibrary), où l'éditeur ouvre, enregistre, renomme et
    // supprime ; les règles des noms (MusicNames), une seule source avec la bibliothèque.
    api(projects.core.data)
    // `api` : la fabrique du ViewModel, construite par app, reçoit le lecteur de musique de core:audio (« Écouter »).
    api(projects.core.audio)
    // `api` : ComposerViewModel est un ViewModel, et app construit sa fabrique.
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Le brouillon gardé dans le SavedStateHandle de l'entrée de navigation (rotation, mort du processus).
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    // Retour, le geste du système compris, demande avant de quitter une musique modifiée (même mécanisme que NavDisplay).
    implementation(libs.androidx.navigationevent)
    implementation(libs.androidx.navigationevent.compose)
    // Le dispatcher de la génération du dé, que la fabrique publique reçoit (P4.20).
    api(libs.kotlinx.coroutines.core)
    // `collectAsStateWithLifecycle` lit le `LifecycleOwner` courant : il faut lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    // `@StringRes` sur les libellés des onglets, des modes et des noms de notes.
    implementation(libs.androidx.annotation)

    // Le faux lecteur de musique, partagé avec les autres features.
    testImplementation(testFixtures(projects.core.audio))
    // Le faux des musiques enregistrées et MainDispatcherExtension, partagés avec les autres features.
    testImplementation(testFixtures(projects.core.data))
    // Le contraste de WCAG et les palettes nommées, partagés avec les autres features.
    testImplementation(testFixtures(projects.core.designsystem))
    // Le ViewModel de l'écran y est construit avec les faux des tests unitaires (lecteur, musiques enregistrées).
    androidTestImplementation(testFixtures(projects.core.audio))
    androidTestImplementation(testFixtures(projects.core.data))
    // Les lignes d'un texte affiché (mots entiers en police agrandie), partagées avec les fenêtres.
    androidTestImplementation(testFixtures(projects.core.designsystem))
    // Le test de la fabrique sauve puis restaure l'état d'une entrée, comme après la mort du processus.
    androidTestImplementation(libs.androidx.savedstate)
    // L'instrumentation, qui fait tourner la fabrique sur le fil principal (ComposerViewModelFactoryTest).
    androidTestImplementation(libs.androidx.test.monitor)
    // L'activité des tests à la version de l'application : elle fournit le répartiteur du retour (NavigationBackHandler).
    androidTestRuntimeOnly(libs.androidx.activity)
    // Le test de l'entrée la compose dans un NavDisplay décoré comme dans app (ADR 0013).
    androidTestImplementation(libs.androidx.navigation3.ui)
    androidTestImplementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // LifecycleRegistry : le cycle de vie de l'entrée simulée par le test de la fabrique.
    androidTestImplementation(libs.androidx.lifecycle.runtime)
}
