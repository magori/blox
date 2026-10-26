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

    // La partie du Serrurier (core:locksmith) et ses positions et rotations (core:game).
    implementation(projects.core.locksmith)
    implementation(projects.core.game)
    // Les cases de la serrure (P7.10).
    implementation(projects.core.board)
    // Les chiffres romains des accords, sur les pilules de l'écran.
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
}
