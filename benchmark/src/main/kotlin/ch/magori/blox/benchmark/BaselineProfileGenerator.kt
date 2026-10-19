package ch.magori.blox.benchmark

import ch.magori.blox.benchmark.junit.BaselineProfileExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.time.Duration.Companion.seconds

/**
 * Génère le profil de référence de l'application (P5.17) : le code du démarrage, de l'accueil et
 * des premières secondes de partie (`GameScreen`, `BoardCanvas`, séquenceur, moteur) est compilé
 * à l'installation plutôt qu'interprété.
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
}
