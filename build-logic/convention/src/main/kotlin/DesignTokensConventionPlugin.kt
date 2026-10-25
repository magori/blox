import ch.magori.blox.buildlogic.library
import ch.magori.blox.buildlogic.libs
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
 * `docs/design-system/tokens.json` applique ce plugin en plus de son plugin principal. Ses tests JVM lisent le
 * fichier, dont le chemin leur est donné par la propriété système `blox.designTokens`, et comparent chaque jeton au
 * code.
 *
 * Le fichier est une entrée des tâches de test, à chemin relatif : le modifier relance les tests, y compris depuis le
 * cache de build, et la clé de cache ne dépend pas de l'emplacement de la copie du dépôt (P5.31). Le chemin absolu
 * n'est passé qu'en argument de la JVM des tests, que Gradle ne compte pas dans la clé.
 *
 * Les tests lisent le JSON avec `kotlinx-serialization-json`, en dépendance de test seulement.
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
            dependencies.add("testImplementation", libs.library("kotlinx-serialization-json"))
        }
    }

    private companion object {
        const val TOKENS_FILE = "docs/design-system/tokens.json"
    }
}

/** Le fichier des jetons, en entrée de la tâche de test, et son chemin pour la JVM des tests. */
abstract class DesignTokensArgument : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val file: RegularFileProperty

    override fun asArguments(): Iterable<String> = listOf("-Dblox.designTokens=${file.get().asFile.absolutePath}")
}
