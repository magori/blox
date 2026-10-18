package ch.magori.blox.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("Bibliothèque absente du catalogue : $alias") }

internal fun VersionCatalog.version(alias: String): String = findVersion(alias)
    .orElseThrow { IllegalStateException("Version absente du catalogue : $alias") }
    .requiredVersion

internal fun VersionCatalog.intVersion(alias: String): Int = version(alias).toInt()
