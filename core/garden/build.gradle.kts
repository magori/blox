plugins {
    alias(libs.plugins.blox.jvm.library)
}

// Domaine du Jardin musical (ADR 0027) : seulement core:game et core:music, en api, ni Android ni
// coroutines. `api(projects.core.music)` arrive avec les ambiances (P10.5), son premier usage :
// buildHealth refuse une dépendance déclarée sans usage.
dependencies {
    api(projects.core.game)
}
