plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.daily"
}

dependencies {
    // `api` : DailyViewModel, que la fabrique construit pour app, est un ViewModel ; la fabrique reçoit de app le dépôt
    // des résultats du jour.
    api(libs.androidx.lifecycle.viewmodel)
    api(projects.core.data)

    // Les 7 premières pièces de la graine, tirées comme celles de la partie (ClassicRandomizer.stable).
    implementation(projects.core.game)
    // Les pièces dessinées comme la pièce suivante d'une partie (NextPieceCanvas).
    implementation(projects.core.board)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // `LifecycleStartEffect` référence `LifecycleOwner` : il faut lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)

    // Les résultats du jour, faux, et MainDispatcherExtension, partagés avec les autres features.
    testImplementation(testFixtures(projects.core.data))
    // L'instrumentation, qui donne les chaînes de l'écran aux tests ; `@StringRes` sur ces chaînes.
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.annotation)
    // Une entrée qui n’est plus au premier plan (LifecycleRegistry) : un second appui ne compte pas.
    androidTestImplementation(libs.androidx.lifecycle.runtime)
    // Les lignes d’un texte affiché (rappel de Jouer entier à 360 dp), partagées avec les autres features.
    androidTestImplementation(testFixtures(projects.core.designsystem))
}
