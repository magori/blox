plugins {
    alias(libs.plugins.blox.android.feature)
}

android {
    namespace = "ch.magori.blox.feature.composer"
}

dependencies {
    // Le modèle de l'éditeur lit et écrit des partitions (Score, Voice, Note, Pitch, Scale, Category).
    implementation(projects.core.music)
    // Les règles des noms des musiques enregistrées (MusicNames de core:data), une seule source avec la bibliothèque.
    implementation(projects.core.data)
}
