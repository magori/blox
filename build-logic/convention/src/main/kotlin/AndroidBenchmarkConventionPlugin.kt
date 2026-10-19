import androidx.baselineprofile.gradle.producer.BaselineProfileProducerExtension
import ch.magori.blox.buildlogic.configureDependencyAnalysis
import ch.magori.blox.buildlogic.configureKotlinAndroid
import ch.magori.blox.buildlogic.configureKtlint
import ch.magori.blox.buildlogic.intVersion
import ch.magori.blox.buildlogic.library
import ch.magori.blox.buildlogic.libs
import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Module de mesure (`com.android.test`) : Macrobenchmark et génération du profil de référence
 * (plugin `androidx.baselineprofile`, côté producteur). Le module déclare lui-même son `namespace`,
 * l'application mesurée (`targetProjectPath`) et ses bibliothèques (Macrobenchmark, UiAutomator).
 *
 * Le plugin `de.mannodermaus.android-junit` ne prend en charge que les modules application,
 * bibliothèque et fonctionnalité dynamique : JUnit 5 est donc branché ici à la main, comme il le
 * ferait (lanceur `android-test-runner` désigné par l'argument `runnerBuilder`, ADR 0009). Le code
 * de mesure vit dans le jeu de sources `main` d'un module de test, d'où `implementation`.
 *
 * Le plugin `androidx.baselineprofile` crée les types de build `nonMinifiedRelease` (génération du
 * profil) et `benchmarkRelease` (mesures), assortis à ceux de l'application, et passe aux tests
 * l'identifiant de l'application cible (`androidx.benchmark.targetPackageName`). Il ne lance que les
 * tests dont la règle figure dans l'argument `androidx.benchmark.enabledRules`, qu'il pose par type
 * de build (voir `RuleExtension` dans le module).
 */
class AndroidBenchmarkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.test")
            pluginManager.apply("androidx.baselineprofile")

            extensions.configure<TestExtension> {
                configureKotlinAndroid(this)
                // Pas de `configureAndroidLint` : AGP ne crée aucune tâche lint pour un module `com.android.test`
                // (ni `lintDebug`, ni `lint`), le réglage n'aurait aucun effet. Les avertissements du compilateur
                // Kotlin y sont bloquants comme ailleurs (`configureKotlinAndroid`, P5.25).
                defaultConfig.targetSdk = libs.intVersion("targetSdk")
                defaultConfig.testInstrumentationRunnerArguments["runnerBuilder"] = JUNIT5_RUNNER_BUILDER
                // Le module s'instrumente lui-même : Macrobenchmark doit pouvoir tuer et relancer
                // l'application mesurée sans être tué avec elle.
                experimentalProperties["android.experimental.self-instrumenting"] = true
                // Chaque artefact JUnit 5 porte sa licence au même chemin ; android-junit les écarte
                // de même dans les APK de test des autres modules.
                packaging.resources.excludes += "/META-INF/LICENSE*"
            }
            extensions.configure<BaselineProfileProducerExtension> {
                // L'appareil branché (téléphone ou émulateur), pas d'appareil géré par Gradle.
                useConnectedDevices = true
            }
            configureKtlint()
            configureDependencyAnalysis()

            dependencies {
                add("implementation", platform(libs.library("junit-bom")))
                add("implementation", libs.library("junit-jupiter-api"))
                add("runtimeOnly", libs.library("junit-jupiter-engine"))
                add("runtimeOnly", libs.library("junit-platform-launcher"))
                add("runtimeOnly", libs.library("android-junit-test-runner"))
            }
        }
    }

    private companion object {
        /** Le constructeur de lanceurs JUnit 5 d'`android-test-runner`, branché sur AndroidJUnitRunner. */
        const val JUNIT5_RUNNER_BUILDER = "de.mannodermaus.junit5.AndroidJUnitFrameworkBuilder"
    }
}
