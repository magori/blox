pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Télécharge le JDK de la toolchain s'il manque sur la machine (voir build-logic/convention).
// Version non lisible depuis le catalogue ici : les plugins de settings se résolvent avant lui.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "blox"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:game")
include(":core:music")
include(":core:audio")
include(":core:haptics")
include(":core:data")
include(":core:designsystem")

include(":feature:home")
include(":feature:game")
include(":feature:composer")

// Mesures (Macrobenchmark) et génération du profil de référence de l'application (P5.17).
include(":benchmark")
