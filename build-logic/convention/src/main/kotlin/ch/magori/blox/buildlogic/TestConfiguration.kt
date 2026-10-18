package ch.magori.blox.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Tests des modules Kotlin purs : JUnit 5 + AssertJ. */
internal fun Project.configureJvmTests() {
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testImplementation", libs.library("assertj-core"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

/**
 * Tests unitaires des modules Android : JUnit 4 (support natif d'AGP) + AssertJ,
 * coroutines-test et Turbine pour les flux. Voir ADR 0003.
 */
internal fun Project.configureAndroidUnitTests() {
    dependencies {
        add("testImplementation", libs.library("junit4"))
        add("testImplementation", libs.library("assertj-core"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
        add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
        add("androidTestImplementation", libs.library("androidx-test-espresso-core"))
    }
}
