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
            // Jeux de sources de test : analysés comme la production, et une dépendance de test
            // inutilisée fait échouer la CI. Seules sont exclues les bibliothèques que les plugins
            // de convention posent dans chaque module, qu'il ait des tests ou non : toute
            // bibliothèque de test ajoutée à `build-logic` (TestConfiguration.kt, configuration
            // Compose, plugin de feature) doit l'être ici aussi. Une règle `onUnusedDependencies`
            // explicite par jeu de sources : sans elle, celle de `all` (fail) s'applique.
            sourceSet("test") {
                onUnusedDependencies {
                    exclude(
                        libs.junit.jupiter.api,
                        libs.junit.jupiter.params,
                        libs.assertj.core,
                        libs.kotlinx.coroutines.test,
                        libs.turbine,
                    )
                    // Posées par le plugin `de.mannodermaus.android-junit` dans les modules Android.
                    exclude(
                        "de.mannodermaus.junit5:android-test-core",
                        "de.mannodermaus.junit5:android-test-extensions",
                    )
                }
            }
            sourceSet("androidTest") {
                onUnusedDependencies {
                    exclude(
                        libs.junit.jupiter.api,
                        libs.junit.jupiter.params,
                        libs.assertj.core,
                        libs.androidx.test.espresso.core,
                        // Posée par le plugin de feature. Le bundle « compose » masque déjà l'avis
                        // (groupe androidx.compose.ui), l'exclusion le rend explicite.
                        libs.androidx.compose.ui.test.android,
                    )
                    // Posées par le plugin `de.mannodermaus.android-junit` (compose : modules Compose).
                    exclude(
                        "de.mannodermaus.junit5:android-test-compose",
                        "de.mannodermaus.junit5:android-test-core",
                        "de.mannodermaus.junit5:android-test-extensions",
                    )
                }
                // Le lanceur d'instrumentation (runner, core) arrive par le plugin android-junit.
                onUsedTransitiveDependencies { exclude("androidx.test:runner", "androidx.test:core") }
            }
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
