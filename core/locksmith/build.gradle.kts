plugins {
    alias(libs.plugins.blox.jvm.library)
    // Doublures de test partagées (`src/testFixtures`) : feature:locksmith les consomme avec
    // `testImplementation(testFixtures(projects.core.locksmith))`.
    alias(libs.plugins.blox.test.fixtures)
}

// Domaine du Serrurier (ADR 0025) : seulement core:game et core:music, ni Android ni coroutines.
dependencies {
    api(projects.core.game)
    api(projects.core.music)

    // Les doublures exposent les orientations et les pièces de core:game.
    testFixturesApi(projects.core.game)
}
