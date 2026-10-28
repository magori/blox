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

    // Le jardin et son ambiance (ADR 0027).
    implementation(projects.core.garden)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Le faux jardin gardé de core:data et MainDispatcherExtension.
    testImplementation(testFixtures(projects.core.data))

    // L'instrumentation, qui donne les chaînes de l'écran aux tests ; `@StringRes` sur ces chaînes.
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.annotation)
    // Une entrée qui n’est plus au premier plan (LifecycleRegistry) : Changer de jeu ne compte plus.
    androidTestImplementation(libs.androidx.lifecycle.runtime)
}
