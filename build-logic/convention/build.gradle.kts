plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

group = "ch.magori.blox.buildlogic"

// Même version de Java que les modules du projet, lue dans le catalogue (ADR 0010).
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.android.junit.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.android.compose.screenshot.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.blox.android.application.asProvider().get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = libs.plugins.blox.android.application.compose.get().pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.blox.android.library.asProvider().get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = libs.plugins.blox.android.library.compose.get().pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.blox.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidScreenshot") {
            id = libs.plugins.blox.android.screenshot.get().pluginId
            implementationClass = "AndroidScreenshotConventionPlugin"
        }
        register("testFixtures") {
            id = libs.plugins.blox.test.fixtures.get().pluginId
            implementationClass = "TestFixturesConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.blox.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}

// Même ktlint que les modules du projet : `./gradlew -p build-logic ktlintCheck`.
ktlint {
    version.set(libs.versions.ktlint.get())
}

// Même parade que `keepKtlintCacheInsideCheckout` (KtlintConfiguration.kt, P5.31) : la sortie en
// cache des tâches ktlint cite des chemins absolus, la clé doit donc dépendre de la copie du dépôt.
val checkoutPath: String = layout.projectDirectory.asFile.absolutePath
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
    inputs.property("projectDirectoryAbsolutePath", checkoutPath)
}
