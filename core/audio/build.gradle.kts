plugins {
    alias(libs.plugins.blox.android.library)
    // Le faux lecteur de musique (FakeMusicPlayer), partagé par les tests des features qui jouent de la musique.
    alias(libs.plugins.blox.test.fixtures)
}

android {
    namespace = "ch.magori.blox.core.audio"
}

dependencies {
    // L'API publique reçoit des musiques de core:music (partitions, fichiers du joueur), et le séquenceur une
    // portée et des dispatchers de coroutines.
    api(projects.core.music)
    api(libs.kotlinx.coroutines.core)

    // @RequiresApi : l'écriture des sonneries par MediaStore demande Android 10 (ADR 0031).
    implementation(libs.androidx.annotation)

    // Le faux lecteur reçoit et note des musiques de core:music.
    testFixturesApi(projects.core.music)

    // Les empreintes de P14.2 confrontent leurs listes figées (accords d'un cadenas, hauteurs du Jardin) au domaine.
    testImplementation(projects.core.garden)
    testImplementation(projects.core.locksmith)

    // Le test instrumenté de MediaPlayer lit le contexte de l'application testée (InstrumentationRegistry).
    androidTestImplementation(libs.androidx.test.monitor)
}
