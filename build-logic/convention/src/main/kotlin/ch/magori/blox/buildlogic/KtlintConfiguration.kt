package ch.magori.blox.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Analyse statique du style Kotlin avec ktlint, réglée par `.editorconfig` à la racine.
 * `ktlintCheck` échoue en CI ; `ktlintFormat` corrige en local.
 */
internal fun Project.configureKtlint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<KtlintExtension> {
        version.set(libs.version("ktlint"))
    }
}
