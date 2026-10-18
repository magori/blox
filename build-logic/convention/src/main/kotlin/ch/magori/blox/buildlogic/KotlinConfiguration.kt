package ch.magori.blox.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

private val JAVA_VERSION = JavaVersion.VERSION_17
private val JVM_TARGET = JvmTarget.JVM_17

/**
 * Réglages communs à tous les modules Android (application et bibliothèques).
 * Kotlin est compilé par le support intégré d'AGP 9 (ADR 0010) : le plugin `org.jetbrains.kotlin.android`
 * n'est plus appliqué, et l'extension `kotlin` exposée par AGP est une [KotlinAndroidExtension].
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.intVersion("compileSdk")
        testOptions.animationsDisabled = true

        defaultConfig.apply {
            minSdk = libs.intVersion("minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions.apply {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
    }

    extensions.configure<KotlinAndroidExtension> {
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
            allWarningsAsErrors.set(false)
        }
    }
}

/** Réglages des modules Kotlin purs, sans dépendance Android. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }

    extensions.configure<KotlinJvmProjectExtension> {
        explicitApiWarning()
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
        }
    }
}
