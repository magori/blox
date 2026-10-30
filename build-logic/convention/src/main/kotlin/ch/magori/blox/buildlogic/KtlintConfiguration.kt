package ch.magori.blox.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

/**
 * Analyse statique du style Kotlin avec ktlint, réglée par `.editorconfig` à la racine.
 * `ktlintCheck` échoue en CI ; `ktlintFormat` corrige en local.
 *
 * Les imports inutilisés, que le compilateur Kotlin ne signale pas, sont refusés par la règle
 * `no-unused-imports` : dépréciée, elle ne tourne que si `.editorconfig` l'active (P5.55).
 */
internal fun Project.configureKtlint() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<KtlintExtension> {
        version.set(libs.version("ktlint"))
    }
    keepKtlintCacheInsideCheckout()
}

/**
 * Lie la clé de cache des tâches d'analyse ktlint au chemin absolu du module (P5.31).
 *
 * Ces tâches (`runKtlintCheckOver…`, `runKtlintFormatOver…`) sont en cache et déclarent leurs
 * sources en chemins relatifs, mais leur sortie (`build/intermediates/ktLint/<tâche>_errors.bin`) stocke
 * les chemins **absolus** des fichiers analysés. Deux copies du dépôt (worktrees) qui partagent le
 * cache de build s'échangeaient donc cette sortie :
 * - la vérification (`runKtlintCheckOver…`), à son exécution incrémentale suivante, réanalyse
 *   chaque fichier cité dans la sortie précédente, donc les fichiers de l'autre copie, dans leur
 *   état du moment ;
 * - le formatage (`runKtlintFormatOver…`) ne relit pas cette sortie (il s'appuie sur un instantané
 *   en `@LocalState`, hors cache), mais quand il la reprend du cache il ne reformate rien et son
 *   rapport cite les fichiers de l'autre copie.
 *
 * Défaut connu du plugin 14.2.0 (issues ktlint-gradle #532 et #1041, correctif
 * proposé dans la PR #1074, non publié).
 *
 * Le chemin du module en entrée garde le cache dans une même copie et, à la première exécution
 * après ce réglage, force une analyse complète qui purge une sortie déjà contaminée. À retirer
 * quand une version du plugin stockera des chemins relatifs.
 */
private fun Project.keepKtlintCacheInsideCheckout() {
    val checkoutPath = layout.projectDirectory.asFile.absolutePath
    tasks.withType<BaseKtLintCheckTask>().configureEach {
        inputs.property("projectDirectoryAbsolutePath", checkoutPath)
    }
}
