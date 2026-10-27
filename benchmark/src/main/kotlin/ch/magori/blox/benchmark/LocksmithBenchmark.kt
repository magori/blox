package ch.magori.blox.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import ch.magori.blox.benchmark.junit.MacrobenchmarkExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Durée des images du premier Serrurier après un démarrage à froid, de l'appui sur sa cartouche à la dernière pose
 * (première composition de l'écran de la serrure comprise), sans profil puis avec le profil de référence seul
 * (compilation partielle).
 *
 * Chaque itération mesure une serrure différente : `OpenCartridge` (`app`) tire sa graine au hasard. Les pièces, le
 * trou et donc les poses acceptées ou refusées changent d'une itération à l'autre, d'où un écart entre elles.
 *
 * `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`, sur un téléphone de préférence.
 */
class LocksmithBenchmark {
    @JvmField
    @RegisterExtension
    val benchmark = MacrobenchmarkExtension()

    @Test
    @DisplayName("first padlock without a profile")
    fun firstPadlockWithoutProfile() = firstPadlock(CompilationMode.None())

    @Test
    @DisplayName("first padlock with the baseline profile")
    fun firstPadlockWithBaselineProfile() =
        firstPadlock(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun firstPadlock(compilationMode: CompilationMode) {
        benchmark.measureRepeated(
            packageName = targetPackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = FRAME_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = { launchFromHomeScreen() },
        ) {
            // La première composition de l'écran de la serrure fait partie de la mesure.
            openLocksmith()
            placePieces(PLACEMENTS)
        }
    }

    private companion object {
        const val PLACEMENTS = 6
    }
}
