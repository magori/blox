plugins {
    alias(libs.plugins.blox.jvm.library)
    // Doublures de test partagées (`src/testFixtures`) : `feature:game` les consomme avec
    // `testImplementation(testFixtures(projects.core.game))`. Plugin du cœur de Gradle, sans réglage.
    `java-test-fixtures`
}

// Domaine du jeu : aucune dépendance, ni Android ni coroutines.
// Toute dépendance ajoutée ici doit faire l'objet d'un ADR.
