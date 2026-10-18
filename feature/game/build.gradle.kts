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
    // La fabrique reçoit aussi le lecteur de musique.
    api(projects.core.audio)
    api(libs.androidx.lifecycle.viewmodel)

    // Le seul module de feature qui a un ViewModel, pour l'instant.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.core)
    // Les partitions que le ViewModel donne au lecteur.
    implementation(projects.core.music)
    // Le retour système de l'écran de jeu (pause, reprise), par le même mécanisme que NavDisplay.
    implementation(libs.androidx.navigationevent)
    implementation(libs.androidx.navigationevent.compose)
}
