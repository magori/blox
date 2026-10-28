plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.garden"
}

dependencies {
    // `api` : GardenViewModel, que la fabrique construit pour app, est un ViewModel.
    api(libs.androidx.lifecycle.viewmodel)
    // `api` : la fabrique reçoit le jardin gardé de core:data (P10.11) et la portée de ses écritures.
    api(projects.core.data)
    api(libs.kotlinx.coroutines.core)
    // `api` : la fabrique reçoit de app le lecteur de musique et le lecteur d’effets sonores (P10.12).
    api(projects.core.audio)

    // Le jardin et son ambiance (ADR 0027).
    implementation(projects.core.garden)
    // La note de chaque case de l’aperçu, en effet sonore (GardenSounds).
    implementation(projects.core.music)
    // Les cases du jardin (GardenCellCanvas), les tuiles du bac (PieceTile), Couleurs seulement (ADR 0023).
    implementation(projects.core.board)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Le faux jardin gardé de core:data et MainDispatcherExtension.
    testImplementation(testFixtures(projects.core.data))
    // Les faux lecteurs de musique et d’effets sonores.
    testImplementation(testFixtures(projects.core.audio))

    // L'instrumentation, qui donne les chaînes de l'écran aux tests ; `@StringRes` sur ces chaînes.
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.annotation)
    // Une entrée qui n’est plus au premier plan (LifecycleRegistry) : Changer de jeu ne compte plus.
    androidTestImplementation(libs.androidx.lifecycle.runtime)
    // Le faux jardin gardé : les parcours par touchers passent par le vrai GardenViewModel.
    androidTestImplementation(testFixtures(projects.core.data))
    androidTestImplementation(testFixtures(projects.core.audio))
}
