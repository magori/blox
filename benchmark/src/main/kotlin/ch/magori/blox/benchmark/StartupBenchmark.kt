package ch.magori.blox.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import ch.magori.blox.benchmark.junit.MacrobenchmarkExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Démarrage à froid jusqu'à la première image de l'accueil, sans profil (tout est interprété ou
 * compilé à la volée) puis avec le profil de référence seul (compilation partielle).
 *
 * `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`, sur un téléphone de préférence.
 */
class StartupBenchmark {
    @JvmField
    @RegisterExtension
    val benchmark = MacrobenchmarkExtension()

    @Test
    @DisplayName("cold startup without a profile")
    fun coldStartupWithoutProfile() = coldStartup(CompilationMode.None())

    @Test
    @DisplayName("cold startup with the baseline profile")
    fun coldStartupWithBaselineProfile() =
        coldStartup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun coldStartup(compilationMode: CompilationMode) {
        benchmark.measureRepeated(
            packageName = targetPackageName,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            iterations = ITERATIONS,
            startupMode = StartupMode.COLD,
        ) {
            launchFromHomeScreen()
        }
    }

    private companion object {
        const val ITERATIONS = 10
    }
}
