plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.musics"
}

dependencies {
    // `api` : la fabrique du ViewModel, construite par app, reçoit la bibliothèque des musiques et les réglages de
    // core:data, le lecteur de musique et l'exportateur des sonneries de core:audio ; MyMusicsViewModel est un
    // ViewModel.
    api(projects.core.audio)
    api(projects.core.data)
    // Les partitions écoutées (Score), les catégories (Category) et l'identité d'une musique à modifier
    // (ScoreId.Personal), dans le rappel public onEdit.
    api(projects.core.music)
    api(libs.androidx.lifecycle.viewmodel)
    // La fabrique reçoit aussi le dispatcher où s'écrivent les sonneries (CoroutineDispatcher).
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // `collectAsStateWithLifecycle` et `dropUnlessResumed` lisent le `LifecycleOwner` courant : il faut
    // lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    // `@StringRes` sur les noms des catégories.
    implementation(libs.androidx.annotation)

    // Le faux lecteur de musique, partagé avec les autres features.
    testImplementation(testFixtures(projects.core.audio))
    // La fausse bibliothèque des musiques et MainDispatcherExtension, partagées avec les autres features.
    testImplementation(testFixtures(projects.core.data))

    // L'instrumentation, qui remet l'appareil en mode tactile avant un test au toucher.
    androidTestImplementation(libs.androidx.test.monitor)
}
