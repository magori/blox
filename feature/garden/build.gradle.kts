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

    // Le jardin et son ambiance (ADR 0027).
    implementation(projects.core.garden)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // L'instrumentation, qui donne les chaînes de l'écran aux tests ; `@StringRes` sur ces chaînes.
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.annotation)
    // Une entrée qui n’est plus au premier plan (LifecycleRegistry) : Changer de jeu ne compte plus.
    androidTestImplementation(libs.androidx.lifecycle.runtime)
}
