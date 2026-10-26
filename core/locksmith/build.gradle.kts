plugins {
    alias(libs.plugins.blox.jvm.library)
}

// Domaine du Serrurier (ADR 0025) : seulement core:game et core:music, ni Android ni coroutines.
dependencies {
    api(projects.core.game)
}
