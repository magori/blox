import ch.magori.blox.buildlogic.library
import ch.magori.blox.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Module de fonctionnalité (un écran ou un parcours) : bibliothèque Android + Compose,
 * clés et entrées Navigation 3 (ADR 0011), et accès au design system. Les dépendances
 * ViewModel et lifecycle arrivent avec le premier ViewModel (P2.2).
 * Une feature ne dépend jamais d'une autre feature (voir docs/architecture.md).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("blox.android.library")
            pluginManager.apply("blox.android.library.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:designsystem"))
                // `api` : les clés NavKey d'une feature sont lues par app, qui assemble la pile (ADR 0011).
                add("api", libs.library("androidx-navigation3-runtime"))
                add("api", libs.library("kotlinx-serialization-core"))
                add("androidTestImplementation", libs.library("androidx-compose-ui-test-android"))
                add("debugRuntimeOnly", libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
