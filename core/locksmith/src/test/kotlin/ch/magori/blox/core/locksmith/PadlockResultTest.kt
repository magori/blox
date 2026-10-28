package ch.magori.blox.core.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PadlockResultTest {
    @Test
    fun `fewer hints is better, even with more moves and refusals`() {
        // Given
        val oneHint = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 1)
        val noHint = PadlockResult(pieces = 3, moves = 9, refusals = 4, hints = 0)

        // Then
        assertThat(noHint.isBetterThan(oneHint)).isTrue
        assertThat(oneHint.isBetterThan(noHint)).isFalse
    }

    @Test
    fun `at equal hints, fewer moves is better, even with more refusals`() {
        // Given
        val moreMoves = PadlockResult(pieces = 3, moves = 5, refusals = 0, hints = 1)
        val fewerMoves = PadlockResult(pieces = 3, moves = 4, refusals = 6, hints = 1)

        // Then
        assertThat(fewerMoves.isBetterThan(moreMoves)).isTrue
        assertThat(moreMoves.isBetterThan(fewerMoves)).isFalse
    }

    @Test
    fun `at equal hints and moves, fewer refusals is better`() {
        // Given
        val moreRefusals = PadlockResult(pieces = 3, moves = 4, refusals = 2, hints = 1)
        val fewerRefusals = PadlockResult(pieces = 3, moves = 4, refusals = 1, hints = 1)

        // Then
        assertThat(fewerRefusals.isBetterThan(moreRefusals)).isTrue
        assertThat(moreRefusals.isBetterThan(fewerRefusals)).isFalse
    }

    @Test
    fun `an equal result is not better`() {
        // Given
        val result = PadlockResult(pieces = 3, moves = 4, refusals = 1, hints = 1)

        // Then
        assertThat(result.isBetterThan(result.copy())).isFalse
    }

    @Test
    fun `a padlock opened in as many moves as pieces, without refusal or hint, is perfect`() {
        // Given
        val result = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0)

        // Then
        assertThat(result.isPerfect).isTrue
    }

    @ParameterizedTest(name = "{0} moves, {1} refusals, {2} hints")
    @CsvSource("4, 0, 0", "3, 1, 0", "3, 0, 1")
    fun `one more move, a refusal or a hint is not perfect`(
        moves: Int,
        refusals: Int,
        hints: Int,
    ) {
        // Given
        val result = PadlockResult(pieces = 3, moves = moves, refusals = refusals, hints = hints)

        // Then
        assertThat(result.isPerfect).isFalse
    }

    @ParameterizedTest(name = "{0} pieces, {1} moves, {2} refusals, {3} hints")
    @CsvSource("0, 1, 0, 0", "3, 3, -1, 0", "3, 3, 0, -1")
    fun `a padlock without pieces or a negative counter is a programming error`(
        pieces: Int,
        moves: Int,
        refusals: Int,
        hints: Int,
    ) {
        assertThatIllegalArgumentException().isThrownBy {
            PadlockResult(pieces = pieces, moves = moves, refusals = refusals, hints = hints)
        }
    }

    @ParameterizedTest(name = "{0} moves")
    @ValueSource(ints = [2, 0, -1])
    fun `a padlock is not opened in fewer moves than pieces`(moves: Int) {
        assertThatIllegalArgumentException().isThrownBy {
            PadlockResult(pieces = 3, moves = moves, refusals = 0, hints = 0)
        }
    }

    @Test
    fun `openings of padlocks of different numbers of pieces are not compared`() {
        // Given
        val threePieces = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val fourPieces = PadlockResult(pieces = 4, moves = 5, refusals = 0, hints = 1)

        // Then
        assertThatIllegalArgumentException().isThrownBy { threePieces.isBetterThan(fourPieces) }
    }
}
