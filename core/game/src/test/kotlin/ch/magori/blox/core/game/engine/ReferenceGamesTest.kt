package ch.magori.blox.core.game.engine

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The reference games ([ReferenceGame]), recorded with the Classic rules before the extraction of `DropTiming` (P11.4):
 * the engine of today must play them tick by tick as the engine of then did. They cover the gravity of slow and fast
 * levels (0, 5, 9, 19, 29), a level up that changes it (0 to 1, 9 to 10), the soft drop, the shift, the rotation, line
 * clears of 1 and 2 rows, a pause, the stack danger and a game over. They do not cover clears of 3 or 4 rows, nor the
 * level up from 18 to 19 (gravity 3 to 2): the games are frozen and never recorded again.
 */
class ReferenceGamesTest {
    @ParameterizedTest
    @ValueSource(strings = ["level-0-to-1", "level-5-game-over", "level-9-to-10", "level-19", "level-29"])
    fun `a reference game of the classic mode is played tick by tick as it was recorded`(name: String) {
        // Given
        val text = checkNotNull(javaClass.getResource("/reference-games/$name.txt")) { "No reference game $name" }
            .readText()
        val recorded = ReferenceGame.recordedTrace(text)

        // When
        val played = ReferenceGame.parse(text).trace(ClassicRules)

        // Then
        val firstDifference = played.indices.firstOrNull { it >= recorded.size || played[it] != recorded[it] }
        firstDifference?.let {
            assertThat(played[it]).describedAs("line ${it + 1} of $name, comments left out")
                .isEqualTo(recorded.getOrNull(it))
        }
        assertThat(played).hasSameSizeAs(recorded)
    }
}
