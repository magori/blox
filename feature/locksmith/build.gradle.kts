plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.locksmith"
}

dependencies {
    // `api` : LocksmithViewModel, que la fabrique construit pour app, est un ViewModel.
    api(libs.androidx.lifecycle.viewmodel)
    // `api` : la fabrique reçoit de app le lecteur d'effets sonores et le dépôt des réglages (P7.13).
    api(projects.core.audio)
    api(projects.core.data)

    // La partie du Serrurier (core:locksmith) et ses positions et rotations (core:game).
    implementation(projects.core.locksmith)
    implementation(projects.core.game)
    // Les cases de la serrure (P7.10).
    implementation(projects.core.board)
    // Les chiffres romains des accords, sur les pilules de l'écran, et les effets sonores du Serrurier (LocksmithSounds).
    implementation(projects.core.music)
    // `api` : la fabrique reçoit de app la portée de l’application, où s’écrit l’ouverture d’une serrure (P7.33).
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // `@StringRes` sur le message de l'écran.
    implementation(libs.androidx.annotation)

    // Le test de restauration encode et relit la clé, comme la navigation après la mort du processus.
    testImplementation(libs.kotlinx.serialization.json)
    // Tourner, poser, ouvrir une serrure dans les tests, comme les tests du domaine.
    testImplementation(testFixtures(projects.core.locksmith))
    // Ouvrir une serrure dans les tests de l’écran, sur l’appareil.
    androidTestImplementation(testFixtures(projects.core.locksmith))
    // L'instrumentation, qui donne les chaînes de l'écran aux tests.
    androidTestImplementation(libs.androidx.test.monitor)
    // Un état sauvegardé fait à la main (`savedState { }`, LocksmithRouteSavingTest), dont les fonctions en ligne
    // passent par core-ktx.
    androidTestImplementation(libs.androidx.savedstate)
    androidTestImplementation(libs.androidx.core.ktx)
    // Le faux lecteur d'effets sonores, le faux dépôt des réglages et MainDispatcherExtension, partagés avec les autres
    // features.
    testImplementation(testFixtures(projects.core.audio))
    testImplementation(testFixtures(projects.core.data))
    // Le parcours de bout en bout de l’écran, sur l’appareil, avec son vrai ViewModel (P7.22).
    androidTestImplementation(testFixtures(projects.core.audio))
    androidTestImplementation(testFixtures(projects.core.data))
    // RecompositionRecorder (P7.22) et les mots entiers des libellés, lus dans leur mise en page (P7.42).
    androidTestImplementation(testFixtures(projects.core.designsystem))
}
