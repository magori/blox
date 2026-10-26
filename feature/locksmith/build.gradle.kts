plugins {
    alias(libs.plugins.blox.android.feature)
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Le test de restauration encode et relit la clé, comme la navigation après la mort du processus.
    testImplementation(libs.kotlinx.serialization.json)
}
