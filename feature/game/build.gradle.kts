plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.game"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit des types du domaine et le dépôt
    // du record, et GameViewModel est un ViewModel.
    api(projects.core.game)
    api(projects.core.data)
    // `api` : la fabrique reçoit de app la musique de chaque choix, (MusicChoice) -> GameMusic?, dont la partition ou
    // le fichier (Music) de GameMusic.Fixed.
    api(projects.core.music)
    // La fabrique reçoit aussi le lecteur de musique.
    api(projects.core.audio)
    // La fabrique reçoit aussi le retour haptique (P5.2).
    api(projects.core.haptics)
    api(libs.androidx.lifecycle.viewmodel)
    // `api` : la fabrique reçoit de app la portée de l'application, où une partie du jour quittée donne ses points
    // (P9.4).
    api(libs.kotlinx.coroutines.core)

    // Le rendu du plateau et de la pièce suivante (P5.73).
    implementation(projects.core.board)

    // Le seul module de feature qui a un ViewModel, pour l'instant.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime)
    // Le SavedStateHandle de la partie, tiré des CreationExtras de son entrée de navigation (P5.13).
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    // Le retour système de l'écran de jeu (pause, reprise), par le même mécanisme que NavDisplay.
    implementation(libs.androidx.navigationevent)
    implementation(libs.androidx.navigationevent.compose)
    // Le faux lecteur de musique, partagé avec l'autre feature.
    testImplementation(testFixtures(projects.core.audio))
    // Le faux retour haptique, partagé avec les tests instrumentés.
    testImplementation(testFixtures(projects.core.haptics))
    // Les faux dépôts de core:data et MainDispatcherExtension, partagés avec l'autre feature.
    testImplementation(testFixtures(projects.core.data))

    // Doublures de test du domaine (ScriptedRandomizer, spawned), partagées par `core:game`.
    testImplementation(testFixtures(projects.core.game))
    // Le contraste de WCAG et les palettes nommées, partagés avec l'autre feature.
    testImplementation(testFixtures(projects.core.designsystem))
    // La clé d'une partie, encodée comme la navigation la sauve : sa date en texte ISO (P9.4).
    testImplementation(libs.kotlinx.serialization.json)
    // Les outils des tests instrumentés du dessin (RecompositionRecorder, drawnContent, assertDrawnAsFresh).
    androidTestImplementation(testFixtures(projects.core.board))
    // Les tests instrumentés emploient ses annotations (ce que buildHealth demande de déclarer).
    androidTestImplementation(libs.androidx.annotation)
    // Le test de la fabrique sauve puis restaure l'état d'une entrée, comme après la mort du processus.
    androidTestImplementation(libs.androidx.savedstate)
    // Le test de restauration compose l'entrée Jeu dans un NavDisplay décoré comme dans app.
    androidTestImplementation(libs.androidx.navigation3.ui)
    androidTestImplementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // Son activité vide (ComponentActivity) porte le test de restauration.
    androidTestImplementation(libs.androidx.activity)
    // L'instrumentation, qui envoie une touche par le système, avec les replis d'Android (B de la manette en retour).
    androidTestImplementation(libs.androidx.test.monitor)
    // La fabrique du ViewModel y est construite avec les faux partagés des tests unitaires (dépôts de core:data,
    // lecteur de core:audio), plutôt qu'avec des copies.
    androidTestImplementation(testFixtures(projects.core.audio))
    androidTestImplementation(testFixtures(projects.core.haptics))
    androidTestImplementation(testFixtures(projects.core.data))
    // Les règles où une rangée est un tick (OneRowPerTickGameEngineFactory) : une fin de partie en quelques secondes
    // d'horloge, et chaque tick visible sur la pièce.
    androidTestImplementation(testFixtures(projects.core.game))
}
