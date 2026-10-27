package ch.magori.blox.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import ch.magori.blox.benchmark.junit.MacrobenchmarkExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.time.Duration.Companion.seconds

/**
 * Durée des images pendant les premières secondes de la première partie après un démarrage à
 * froid, de l'appui sur Jouer à la fin du jeu mesuré (première composition de l'écran de jeu
 * comprise), sans profil puis avec le profil de référence seul (compilation partielle).
 *
 * `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`, sur un téléphone de préférence.
 */
class GameBenchmark {
    @JvmField
    @RegisterExtension
    val benchmark = MacrobenchmarkExtension()

    @Test
    @DisplayName("first game without a profile")
    fun firstGameWithoutProfile() = firstGame(CompilationMode.None())

    @Test
    @DisplayName("first game with the baseline profile")
    fun firstGameWithBaselineProfile() =
        firstGame(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun firstGame(compilationMode: CompilationMode) {
        benchmark.measureRepeated(
            packageName = targetPackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = FRAME_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = { launchFromHomeScreen() },
        ) {
            // La première composition de l'écran de jeu fait partie de la mesure.
            startGame()
            play(PLAYING_TIME)
        }
    }

    private companion object {
        val PLAYING_TIME = 8.seconds
    }
}
