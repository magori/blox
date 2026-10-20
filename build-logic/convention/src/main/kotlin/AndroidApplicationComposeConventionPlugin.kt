import ch.magori.blox.buildlogic.configureAndroidCompose
import ch.magori.blox.buildlogic.library
import ch.magori.blox.buildlogic.libs
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Application Compose. Ses tests instrumentés composent les parcours qui traversent plusieurs features (P5.5) : Compose
 * UI Test, comme dans une feature, et l'activité vide qui les porte (`ui-test-manifest`, dans le build debug seulement).
 */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            configureAndroidCompose(extensions.getByType<ApplicationExtension>())

            dependencies {
                // Exclue aussi dans `build.gradle.kts` (dependencyAnalysis, sourceSet("androidTest")).
                add("androidTestImplementation", libs.library("androidx-compose-ui-test-android"))
                add("debugRuntimeOnly", libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
