package ch.magori.blox.benchmark

import ch.magori.blox.benchmark.junit.BaselineProfileExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.time.Duration.Companion.seconds

/**
 * Génère le profil de référence de l'application (P5.17) : le code du démarrage, du choix du jeu, de l'accueil et
 * des premières secondes de partie (`GameScreen`, `BoardCanvas`, séquenceur, moteur), puis celui du Serrurier
 * (`LocksmithScreen`, `PadlockGame`, sons de la serrure, P7.21) et celui de la Graine du jour (`DailyScreen`, sa
 * partie, P9.7) est compilé à l'installation plutôt qu'interprété.
 * Le plugin `androidx.baselineprofile` fusionne les profils des parcours.
 *
 * `./gradlew :app:generateBaselineProfile`, avec un appareil branché (Android 13 ou plus, ou rooté).
 * Le profil arrive dans `app/src/main/generated/baselineProfiles/`, à versionner.
 *
 * Pas de profil de démarrage (`includeInStartupProfile = false`) : il ne sert qu'à ranger le code
 * du démarrage dans le premier fichier dex (`dexLayoutOptimization`), que le projet n'active pas.
 */
class BaselineProfileGenerator {
    @JvmField
    @RegisterExtension
    val baselineProfile = BaselineProfileExtension()

    @Test
    @DisplayName("startup, home screen and the first seconds of a game")
    fun startupHomeScreenAndFirstSecondsOfAGame() {
        baselineProfile.collect(packageName = targetPackageName, includeInStartupProfile = false) {
            launchFromHomeScreen()
            startGame()
            play(10.seconds)
            pauseGame()
        }
    }

    @Test
    @DisplayName("startup, the entry of the Graine du jour and the first seconds of its game")
    fun startupDailyEntryAndFirstSecondsOfItsGame() {
        baselineProfile.collect(packageName = targetPackageName, includeInStartupProfile = false) {
            launchFromHomeScreen()
            startDailyGame()
            play(10.seconds)
            pauseGame()
        }
    }

    @Test
    @DisplayName("startup and the first placements of a padlock")
    fun startupAndFirstPlacementsOfAPadlock() {
        baselineProfile.collect(packageName = targetPackageName, includeInStartupProfile = false) {
            launchFromHomeScreen()
            openLocksmith()
            placePieces(count = 6)
        }
    }
}
