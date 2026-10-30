plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.home"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit les dépôts de core:data, le lecteur de
    // musique de core:audio (aperçu « Écouter ») et la progression des niveaux de core:game ;
    // HomeViewModel est un ViewModel.
    api(projects.core.audio)
    api(projects.core.data)
    // `api` : la fabrique reçoit de app la partition de chaque choix de musique, (MusicChoice) -> Score?.
    api(projects.core.music)
    api(projects.core.game)
    api(libs.androidx.lifecycle.viewmodel)

    // Les couleurs du logo, celles des pièces (logoColors).
    implementation(projects.core.board)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // La valeur par défaut de `dropUnlessResumed` (LocalLifecycleOwner.current) référence
    // `LifecycleOwner` dans le bytecode de l'écran : il faut donc lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.kotlinx.coroutines.core)
    // `@StringRes` sur les libellés des choix.
    implementation(libs.androidx.annotation)
    // Le sélecteur de documents du système pour « Mon fichier » (ActivityResultContracts.OpenDocument).
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    // Le faux lecteur de musique, partagé avec l'autre feature.
    testImplementation(testFixtures(projects.core.audio))
    // Les faux dépôts de core:data et MainDispatcherExtension, partagés avec l'autre feature.
    testImplementation(testFixtures(projects.core.data))
    // Les lignes d'un texte affiché (mots entiers en police agrandie), partagées avec les autres features.
    androidTestImplementation(testFixtures(projects.core.designsystem))
    // RecompositionRecorder : rien ne recompose pendant que l'aperçu joue (P5.77).
    androidTestImplementation(testFixtures(projects.core.board))
}
