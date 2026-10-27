plugins {
    alias(libs.plugins.blox.android.feature)
    alias(libs.plugins.blox.android.screenshot)
}

android {
    namespace = "ch.magori.blox.feature.cartridges"
}

dependencies {
    // La valeur par défaut de `dropUnlessResumed` (LocalLifecycleOwner.current) référence `LifecycleOwner` dans le
    // bytecode de l'entrée : il faut donc lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)
    // `@StringRes` sur le nom de chaque cartouche.
    implementation(libs.androidx.annotation)
    // La mascotte de chaque jeu, sur sa cartouche, et le logo (V36).
    implementation(projects.core.board)
    // `Flow`, l'autocollant « NOUVELLE » que `app` passe à l'entrée.
    api(libs.kotlinx.coroutines.core)

    // Les lignes d'un texte affiché (mots entiers en police agrandie), partagées avec les autres features.
    androidTestImplementation(testFixtures(projects.core.designsystem))
    // `InstrumentationRegistry`, qui lit les noms des cartouches dans les ressources.
    androidTestImplementation(libs.androidx.test.monitor)
}
