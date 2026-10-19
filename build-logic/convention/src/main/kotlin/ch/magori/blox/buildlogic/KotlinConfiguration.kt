package ch.magori.blox.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Réglages communs à tous les modules Android (application et bibliothèques).
 * Kotlin est compilé par le support intégré d'AGP 9 (ADR 0010) : le plugin `org.jetbrains.kotlin.android`
 * n'est plus appliqué, et l'extension `kotlin` exposée par AGP est une [KotlinAndroidExtension].
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    configureJavaToolchain()
    commonExtension.apply {
        compileSdk = libs.intVersion("compileSdk")
        testOptions.animationsDisabled = true

        defaultConfig.apply {
            minSdk = libs.intVersion("minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions.apply {
            sourceCompatibility = targetJavaVersion
            targetCompatibility = targetJavaVersion
        }
    }

    extensions.configure<KotlinAndroidExtension> {
        compilerOptions.configureCommonKotlinOptions(targetJvm)
        optInExperimentalCoroutinesInUnitTests()
    }
}

/** Réglages des modules Kotlin purs, sans dépendance Android. */
internal fun Project.configureKotlinJvm() {
    configureJavaToolchain()
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = targetJavaVersion
        targetCompatibility = targetJavaVersion
    }

    extensions.configure<KotlinJvmProjectExtension> {
        explicitApi()
        compilerOptions.configureCommonKotlinOptions(targetJvm)
    }
}

/**
 * Options du compilateur communes aux modules Android et Kotlin purs.
 *
 * Production et tests, un avertissement du compilateur Kotlin est une erreur (`allWarningsAsErrors`,
 * P5.25) : rien ne passe la CI sans être vu. Un avertissement assumé se fait taire au plus près,
 * `@Suppress("…")` avec sa raison (voir CLAUDE.md, « Zéro avertissement »).
 */
private fun KotlinJvmCompilerOptions.configureCommonKotlinOptions(target: JvmTarget) {
    jvmTarget.set(target)
    allWarningsAsErrors.set(true)
}

/**
 * `runCurrent`, `advanceUntilIdle`, `UnconfinedTestDispatcher`, `Dispatchers.setMain` : des outils
 * de base de kotlinx-coroutines-test restent marqués `@ExperimentalCoroutinesApi`. Les compilations
 * de tests unitaires Android (`debugUnitTest`, `releaseUnitTest`…), où la convention pose
 * coroutines-test (TestConfiguration.kt), y consentent toutes d'un coup ; ni le code de production,
 * ni les tests instrumentés, ni les tests de captures ne le font.
 *
 * Pas dans les modules Kotlin purs : `core:game` et `core:music` n'ont pas les coroutines, et un
 * marqueur d'opt-in introuvable est un avertissement, donc une erreur (`allWarningsAsErrors`).
 * Le Kotlin intégré d'AGP 9 (ADR 0010) expose les compilations par variante (`target.compilations`),
 * chacune avec sa tâche ; seuls les `kotlin.sourceSets` y sont vides.
 */
private fun KotlinAndroidExtension.optInExperimentalCoroutinesInUnitTests() {
    target.compilations.matching { it.name.endsWith("UnitTest") }.configureEach {
        compileTaskProvider.configure {
            compilerOptions.optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}

/** Version de Java cible, lue dans le catalogue (`java`) : une seule valeur pour tout le projet. */
private val Project.javaVersion: Int
    get() = libs.intVersion("java")

private val Project.targetJavaVersion: JavaVersion
    get() = JavaVersion.toVersion(javaVersion)

private val Project.targetJvm: JvmTarget
    get() = JvmTarget.fromTarget(javaVersion.toString())

/**
 * Toolchain Java : la compilation utilise un JDK de la version cible, quel que soit celui qui
 * lance Gradle. S'il manque, le résolveur foojay déclaré dans `settings.gradle.kts` le
 * télécharge. Les tâches Kotlin suivent la toolchain déclarée sur l'extension `java`.
 * `sourceCompatibility`, `targetCompatibility` et `jvmTarget` restent explicites : la toolchain
 * fixe le JDK qui compile, eux fixent le bytecode produit.
 */
private fun Project.configureJavaToolchain() {
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }
}
