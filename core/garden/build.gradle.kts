plugins {
    alias(libs.plugins.blox.jvm.library)
}

// Domaine du Jardin musical (ADR 0027) : seulement core:game et core:music, en api, ni Android ni
// coroutines.
dependencies {
    api(projects.core.game)
    api(projects.core.music)
}
