package ch.magori.blox.buildlogic

import org.gradle.api.Project

/**
 * Analyse des dépendances déclarées (plugin `com.autonomousapps.dependency-analysis`).
 * Le plugin est chargé et configuré à la racine (`build.gradle.kts`) ; chaque module doit
 * aussi l'appliquer pour être analysé par `buildHealth`.
 */
internal fun Project.configureDependencyAnalysis() {
    pluginManager.apply("com.autonomousapps.dependency-analysis")
}
