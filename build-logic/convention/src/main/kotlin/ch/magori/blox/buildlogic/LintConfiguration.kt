package ch.magori.blox.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

/**
 * Lint des modules Android (application et bibliothèques) : tout avertissement fait échouer
 * `lintDebug` (en CI). Sans cela, un nouvel avertissement (accessibilité, règles Compose de Slack)
 * passerait la CI sans être vu. `lintVitalRelease`, lancé par `assembleRelease`, ne vérifie que les
 * problèmes fatals et lit la même base.
 *
 * Un avertissement assumé se traite au plus près de sa cause, avec sa raison écrite :
 * `tools:ignore` dans le XML, `@Suppress` dans le code, ou `lint.xml` du module pour une règle
 * entière. La base `lint-baseline.xml` d'un module sert aux avertissements qu'on ne peut pas taire
 * à la source (un fichier hors du module, comme le catalogue de versions). Elle n'est branchée que
 * si le fichier existe : lint crée sinon la base manquante et fait échouer le build (le drapeau AGP
 * qui l'éviterait, `android.experimental.lint.missingBaselineIsEmptyBaseline`, est expérimental).
 * Pour en ajouter une : créer un `lint-baseline.xml` vide, puis `./gradlew :module:updateLintBaseline`.
 * Cette tâche régénère la base en effaçant ses commentaires : la raison de chaque entrée est donc
 * aussi écrite ici.
 *
 * Avertissements assumés aujourd'hui (P5.25) :
 * - `OldTargetApi`, base de `app` : `targetSdk` (36) reste sous `compileSdk` (37, imposé par les
 *   métadonnées AAR d'AndroidX) tant que l'application n'a pas été testée sous la nouvelle API.
 * - `LockedOrientationActivity` et `DiscouragedApi`, `tools:ignore` dans le manifeste de `app` :
 *   portrait forcé, en attendant le test sur téléphone (P2.8) et une mise en page paysage.
 */
internal fun Project.configureAndroidLint(commonExtension: CommonExtension) {
    commonExtension.lint.apply {
        warningsAsErrors = true
        abortOnError = true
        file("lint-baseline.xml").takeIf { it.exists() }?.let { baseline = it }
        // Règles qui signalent qu'une version plus récente existe (bibliothèque, plugin, Gradle) :
        // Dependabot ouvre chaque semaine la PR de montée (.github/dependabot.yml). En erreur, elles
        // feraient échouer la CI de main à chaque nouvelle version publiée, sans changement du dépôt.
        disable += setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
        )
    }
}
