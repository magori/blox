package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.tempo.Meter
import ch.magori.blox.core.game.tempo.tempoRules
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The reference games ([ReferenceGame]), recorded with the Classic rules before the extraction of `DropTiming` (P11.4):
 * the engine of today must play them tick by tick as the engine of then did. They cover the gravity of slow and fast
 * levels (0, 5, 9, 19, 29), a level up that changes it (0 to 1, 9 to 10), the soft drop, the shift, the rotation, line
 * clears of 1 and 2 rows, a pause, the stack danger and a game over. They do not cover clears of 3 or 4 rows, nor the
 * level up from 18 to 19 (gravity 3 to 2): the games are frozen and never recorded again.
 *
 * The rules of Au tempo, given back the classic drop timing, play them the same (P11.8). This second case is redundant
 * by construction with `TempoRulesTest`, which proves that the two rules differ only by the drop timing; it is kept as
 * the non-regression asked for by ADR 0028.
 */
class ReferenceGamesTest {
    @ParameterizedTest(name = "classic: {0}")
    @MethodSource("referenceGames")
    fun `a reference game of the classic mode is played tick by tick as it was recorded`(name: String) {
        assertPlayedAsRecorded(name, ClassicRules)
    }

    @ParameterizedTest(name = "tempo, classic drop timing: {0}")
    @MethodSource("referenceGames")
    fun `the reference games are played the same by the tempo rules with the classic drop timing`(name: String) {
        // Any meter: its drop timing is replaced
        val rules = tempoRules(Meter(ticksPerEighth = Ticks(15))).copy(dropTiming = ClassicRules.dropTiming)

        assertPlayedAsRecorded(name, rules)
    }

    private fun assertPlayedAsRecorded(
        name: String,
        rules: Rules,
    ) {
        // Given
        val text = checkNotNull(javaClass.getResource("/reference-games/$name.txt")) { "No reference game $name" }
            .readText()
        val recorded = ReferenceGame.recordedTrace(text)

        // When
        val played = ReferenceGame.parse(text).trace(rules)

        // Then
        val firstDifference = played.indices.firstOrNull { it >= recorded.size || played[it] != recorded[it] }
        firstDifference?.let {
            assertThat(played[it]).describedAs("line ${it + 1} of $name, comments left out")
                .isEqualTo(recorded.getOrNull(it))
        }
        assertThat(played).hasSameSizeAs(recorded)
    }

    private companion object {
        @JvmStatic
        fun referenceGames(): List<String> =
            listOf("level-0-to-1", "level-5-game-over", "level-9-to-10", "level-19", "level-29")
    }
}
