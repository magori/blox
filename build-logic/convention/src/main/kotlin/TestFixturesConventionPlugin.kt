import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Doublures de test partagées (`src/testFixtures`) : un module qui en publie applique ce plugin en plus de
 * son plugin principal, dans n'importe quel ordre. Les autres modules les consomment avec
 * `testImplementation(testFixtures(projects.core.xxx))` (`androidTestImplementation` pour les tests
 * instrumentés), et les tests du module lui-même les voient.
 *
 * - Module Kotlin pur (`blox.jvm.library`) : plugin `java-test-fixtures` du cœur de Gradle.
 * - Bibliothèque Android (`blox.android.library`) : les testFixtures d'AGP.
 *
 * Un module qui n'est ni l'un ni l'autre échoue à la configuration. Les dépendances des doublures restent
 * déclarées par le module (`testFixturesApi`, `testFixturesImplementation`) : elles lui sont propres.
 */
class TestFixturesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin(ANDROID_LIBRARY) {
                extensions.configure<LibraryExtension> { testFixtures.enable = true }
            }
            pluginManager.withPlugin(KOTLIN_JVM) {
                pluginManager.apply("java-test-fixtures")
            }
            afterEvaluate {
                if (!pluginManager.hasPlugin(ANDROID_LIBRARY) && !pluginManager.hasPlugin(KOTLIN_JVM)) {
                    throw GradleException(
                        "blox.test.fixtures s'applique à une bibliothèque : appliquer aussi blox.jvm.library " +
                            "ou blox.android.library dans $path.",
                    )
                }
            }
        }
    }

    private companion object {
        const val ANDROID_LIBRARY = "com.android.library"
        const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
    }
}
