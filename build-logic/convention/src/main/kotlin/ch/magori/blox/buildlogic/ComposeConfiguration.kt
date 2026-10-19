package ch.magori.blox.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/** Fichier de stabilité, à la racine du dépôt : les types des modules Kotlin purs que Compose tient pour stables. */
private const val STABILITY_CONFIGURATION_FILE = "compose-stability.conf"

/**
 * Propriété Gradle qui active les rapports du compilateur Compose (stabilité des classes, composables
 * « skippable » ou non) dans `build/compose-reports` et `build/compose-metrics` de chaque module Compose :
 * `-Pblox.composeReports=true` les active ; `=false`, une autre valeur ou rien les laisse éteints.
 * Lancer avec `--rerun` : `reportsDestination` n'est pas une entrée de la tâche, une compilation à jour
 * ou incrémentale ne réécrirait pas des rapports complets.
 */
private const val COMPOSE_REPORTS_PROPERTY = "blox.composeReports"

/** Active Compose et ajoute les dépendances minimales communes. */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    commonExtension.apply {
        buildFeatures.apply {
            compose = true
        }
    }

    configureComposeCompiler()

    dependencies {
        val bom = libs.library("androidx-compose-bom")
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        add("lintChecks", libs.library("slack-compose-lint-checks"))
    }
}

/**
 * Branche le fichier de stabilité et, sur demande, les rapports. Le plugin Compose ne passe au compilateur
 * que le chemin du fichier : sans l'entrée déclarée ici, modifier son contenu laisse la compilation à jour
 * (et le cache de build la resservir), le changement sans effet jusqu'à la prochaine modification d'une source.
 * Cause dans KGP 2.4.21 : l'option est un `FilesSubpluginOption` de genre `FilesOptionKind.INTERNAL`, que
 * `CompilerPluginConfig.getAsTaskInputArgs` n'expose pas comme entrée de la tâche. À revoir à chaque montée
 * de Kotlin : retirer l'entrée ici si le plugin déclare enfin le fichier.
 */
private fun Project.configureComposeCompiler() {
    val stabilityConfiguration = isolated.rootProject.projectDirectory.file(STABILITY_CONFIGURATION_FILE)
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(stabilityConfiguration)
        if (providers.gradleProperty(COMPOSE_REPORTS_PROPERTY).map(String::toBoolean).getOrElse(false)) {
            reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
            metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
        }
    }
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        inputs.file(stabilityConfiguration)
            .withPropertyName("composeStabilityConfiguration")
            .withPathSensitivity(PathSensitivity.NONE)
    }
}
