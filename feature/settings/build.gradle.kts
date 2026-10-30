plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.settings"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit les réglages et le record de core:data, les effets
    // sonores de core:audio et le retour haptique de core:haptics (un réglage allumé se fait sentir une fois) ;
    // SettingsViewModel est un ViewModel.
    api(projects.core.audio)
    api(projects.core.data)
    api(projects.core.haptics)
    api(libs.androidx.lifecycle.viewmodel)

    // La rotation jouée quand les effets sonores sont rallumés, les volumes (Volume).
    implementation(projects.core.music)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // La valeur par défaut de `dropUnlessResumed` (LocalLifecycleOwner.current) référence
    // `LifecycleOwner` dans le bytecode de l'entrée : il faut donc lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.kotlinx.coroutines.core)

    // Le faux lecteur d'effets sonores, partagé avec les autres features.
    testImplementation(testFixtures(projects.core.audio))
    // Les faux dépôts de core:data et MainDispatcherExtension, partagés avec les autres features.
    testImplementation(testFixtures(projects.core.data))
    // Le faux retour haptique, partagé avec feature:game.
    testImplementation(testFixtures(projects.core.haptics))
    // Le contraste et les palettes nommées (schéma des touches de Gaucher).
    testImplementation(testFixtures(projects.core.designsystem))
    // Les lignes d'un texte affiché (mots entiers en police agrandie), partagées avec les autres features.
    androidTestImplementation(testFixtures(projects.core.designsystem))
}
