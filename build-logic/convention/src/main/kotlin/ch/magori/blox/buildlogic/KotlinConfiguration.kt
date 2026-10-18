package ch.magori.blox.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
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
        compilerOptions {
            jvmTarget.set(targetJvm)
        }
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
        compilerOptions {
            jvmTarget.set(targetJvm)
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
