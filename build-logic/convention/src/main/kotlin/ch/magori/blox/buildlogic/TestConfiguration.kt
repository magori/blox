package ch.magori.blox.buildlogic

import de.mannodermaus.gradle.plugins.junit5.dsl.AndroidJUnitPlatformExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Tests des modules Kotlin purs : JUnit 5 + AssertJ. */
internal fun Project.configureJvmTests() {
    addJUnit5UnitTestDependencies()
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

/**
 * Tests des modules Android, unitaires et instrumentés, sur JUnit 5 grâce au plugin
 * `de.mannodermaus.android-junit` ; AssertJ, coroutines-test et Turbine pour les flux.
 * Le plugin branche lui-même JUnit Platform sur les tâches de test et fournit les
 * extensions d'instrumentation (ActivityScenario, Compose). Voir ADR 0009.
 */
internal fun Project.configureAndroidTests() {
    pluginManager.apply("de.mannodermaus.android-junit")
    addJUnit5UnitTestDependencies()
    dependencies {
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
        add("androidTestImplementation", platform(libs.library("junit-bom")))
        add("androidTestImplementation", libs.library("junit-jupiter-api"))
        add("androidTestImplementation", libs.library("assertj-core"))
        add("androidTestImplementation", libs.library("androidx-test-espresso-core"))
    }
    extensions.configure<AndroidJUnitPlatformExtension> {
        instrumentationTests.includeExtensions.set(true)
    }
}

private fun Project.addJUnit5UnitTestDependencies() {
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testImplementation", libs.library("assertj-core"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
    }
}
