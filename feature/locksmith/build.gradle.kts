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
    implementation(libs.kotlinx.coroutines.core)
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
    // Le faux lecteur d'effets sonores, le faux dépôt des réglages et MainDispatcherExtension, partagés avec les autres
    // features.
    testImplementation(testFixtures(projects.core.audio))
    testImplementation(testFixtures(projects.core.data))
}
