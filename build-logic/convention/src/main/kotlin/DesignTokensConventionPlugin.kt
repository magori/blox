import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

/**
 * Jetons du design system (ADR 0019) : un module dont le code reprend des jetons de
 * `docs/design-system/tokens.json` applique ce plugin en plus de son plugin principal, dans n'importe quel ordre.
 * Ses tests JVM lisent le fichier, dont le chemin leur est donné par la propriété système `blox.designTokens`, et
 * comparent chaque jeton au code.
 *
 * Le fichier est une entrée des tâches de test, à chemin relatif : le modifier relance les tests, y compris depuis le
 * cache de build, et la clé de cache ne dépend pas de l'emplacement de la copie du dépôt (P5.31). Le chemin absolu
 * n'est passé qu'en argument de la JVM des tests, que Gradle ne compte pas dans la clé.
 *
 * Un seul lecteur du fichier, `DesignTokens`, dans les doublures de test de `core:designsystem`, seules à dépendre de
 * `kotlinx-serialization-json` : les tests de `core:designsystem` le voient, un autre module le déclare
 * (`testImplementation(testFixtures(projects.core.designsystem))`, `core:board` pour les jetons des pièces).
 */
class DesignTokensConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val tokens = objects.newInstance<DesignTokensArgument>().apply {
                file.set(isolated.rootProject.projectDirectory.file(TOKENS_FILE))
            }
            tasks.withType<Test>().configureEach {
                jvmArgumentProviders.add(tokens)
            }
            // Le plugin principal peut être appliqué avant ou après celui-ci : on vérifie une fois le module évalué.
            afterEvaluate {
                if (!pluginManager.hasPlugin(ANDROID_LIBRARY) && !pluginManager.hasPlugin(KOTLIN_JVM)) {
                    throw GradleException(
                        "blox.design.tokens s'applique à une bibliothèque : appliquer aussi blox.jvm.library " +
                            "ou blox.android.library dans $path.",
                    )
                }
            }
        }
    }

    private companion object {
        const val TOKENS_FILE = "docs/design-system/tokens.json"
        const val ANDROID_LIBRARY = "com.android.library"
        const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
    }
}

/** Le fichier des jetons, en entrée de la tâche de test, et son chemin pour la JVM des tests. */
abstract class DesignTokensArgument : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val file: RegularFileProperty

    override fun asArguments(): Iterable<String> = listOf("-Dblox.designTokens=${file.get().asFile.absolutePath}")
}
