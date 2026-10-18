import ch.magori.blox.buildlogic.configureJvmTests
import ch.magori.blox.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Module Kotlin pur, sans aucune dépendance Android : c'est là que vit le domaine.
 * Testé avec JUnit 5 et AssertJ, exécutable sur la JVM sans émulateur.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            configureJvmTests()
        }
    }
}
