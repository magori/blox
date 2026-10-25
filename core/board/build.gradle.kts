plugins {
    alias(libs.plugins.blox.android.library)
    alias(libs.plugins.blox.android.library.compose)
    // Les outils des tests instrumentés du dessin (RecompositionRecorder, drawnContent, assertDrawnAsFresh), partagés
    // avec l'écran de jeu.
    alias(libs.plugins.blox.test.fixtures)
    // Le plateau seul, rendu sur la JVM et comparé à sa référence.
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.core.board"
}

dependencies {
    // `api` : le plateau et la pièce suivante se dessinent à partir des types du domaine et du thème.
    api(projects.core.game)
    api(projects.core.designsystem)

    androidTestImplementation(libs.androidx.compose.ui.test.android)
    debugRuntimeOnly(libs.androidx.compose.ui.test.manifest)

    testFixturesApi(libs.androidx.compose.ui.test.android)
    testFixturesApi(libs.android.junit.test.compose)
    testFixturesImplementation(libs.assertj.core)
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
}
