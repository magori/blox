// Les plugins sont déclarés ici pour être chargés une seule fois dans le classloader racine et
// partagés par tous les modules ; seul `dependency-analysis` est aussi appliqué ici, il l'exige.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.junit) apply false
    alias(libs.plugins.ktlint) apply false
    // Analyse des dépendances déclarées : configurée ici, appliquée dans chaque module par les
    // plugins de convention, rapport `buildHealth` (tâche P0.9).
    alias(libs.plugins.dependency.analysis)
}

dependencyAnalysis {
    structure {
        // Compose arrive par `core:designsystem` (api sur foundation et material3) ; le bundle
        // évite un conseil « transitif utilisé » par artefact (ui, runtime, foundation-layout…).
        bundle("compose") {
            includeGroup("androidx.compose.animation")
            includeGroup("androidx.compose.foundation")
            includeGroup("androidx.compose.material3")
            includeGroup("androidx.compose.runtime")
            includeGroup("androidx.compose.ui")
        }
    }
    issues {
        all {
            // Les bibliothèques de test viennent des plugins de convention, avant les tests.
            ignoreSourceSet("test", "androidTest")
            onUnusedDependencies {
                // Une dépendance de production déclarée sans usage fait échouer la CI…
                severity("fail")
                // …sauf ui-tooling-preview, posé par le plugin Compose sur des modules sans @Preview.
                exclude("androidx.compose.ui:ui-tooling-preview")
            }
            onUsedTransitiveDependencies { severity("warn") }
            onIncorrectConfiguration {
                severity("warn")
                // Faux positif : des previews `private` ne font pas de ui-tooling-preview une API.
                exclude("androidx.compose.ui:ui-tooling-preview")
            }
            onRuntimeOnly { severity("warn") }
        }
    }
}
