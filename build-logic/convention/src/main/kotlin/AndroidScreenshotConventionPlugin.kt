import ch.magori.blox.buildlogic.library
import ch.magori.blox.buildlogic.libs
import com.android.build.api.dsl.CommonExtension
import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Tests de captures d'écran à partir de previews Compose (plugin officiel « Compose Preview
 * Screenshot Testing », expérimental) : les fonctions `@PreviewTest` du jeu de sources
 * `src/screenshotTest` sont rendues sur la JVM, sans émulateur, et comparées aux PNG de référence
 * versionnés dans `src/screenshotTestDebug/reference`.
 *
 * - `./gradlew validateDebugScreenshotTest` compare les captures aux références, rapport dans
 *   `build/reports/screenshotTest/preview/debug/index.html`.
 * - Après un changement voulu de l'interface, ou après avoir ajouté, renommé ou retiré une
 *   capture : `./gradlew updateDebugScreenshotTest` régénère les références. Supprimer à la main
 *   les PNG des captures retirées ou renommées, que la tâche laisse en place, puis relancer la
 *   validation et versionner les PNG avec le changement.
 *
 * Une capture peut différer de [IMAGE_DIFFERENCE_THRESHOLD] de sa référence : une marge, car le rendu
 * mesuré en P5.8 est identique au pixel près sous Windows, où les références sont générées, et sous Linux.
 *
 * Le moteur de test du plugin tourne sur JUnit Platform : aucune dépendance JUnit 4 (ADR 0009).
 * À appliquer à un module Android (bibliothèque, feature ou application) en plus de son plugin
 * principal, qui doit être appliqué avant.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val android = extensions.findByType(CommonExtension::class.java)
                ?: throw GradleException(
                    "blox.android.screenshot s'applique à un module Android : appliquer d'abord " +
                        "blox.android.library, blox.android.feature ou blox.android.application dans $path.",
                )
            // alpha16 exige ce réglage du module en plus du drapeau global de gradle.properties, et
            // le vérifie dès que son plugin est appliqué : il vient donc avant.
            android.experimentalProperties["android.experimental.enableScreenshotTest"] = true
            pluginManager.apply("com.android.compose.screenshot")
            // alpha16 n'a pas de DSL pour la tolérance : c'est une entrée de ses tâches de validation (0 par défaut).
            tasks.withType<PreviewScreenshotValidationTask>().configureEach {
                testEngineInput.threshold.set(IMAGE_DIFFERENCE_THRESHOLD)
            }

            dependencies {
                add("screenshotTestImplementation", libs.library("screenshot-validation-api"))
                add("screenshotTestImplementation", libs.library("androidx-compose-ui-tooling"))
            }
        }
    }

    private companion object {
        /** La part de l'image qui peut différer de la référence : 0,05 %. */
        const val IMAGE_DIFFERENCE_THRESHOLD = 0.0005f
    }
}
