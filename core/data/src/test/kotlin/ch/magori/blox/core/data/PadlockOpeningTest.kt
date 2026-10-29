package ch.magori.blox.core.data

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PadlockOpeningTest {
    @Test
    fun `fewer hints is better, even with more moves and refusals`() {
        // Given
        val withoutHint = PadlockOpening(pieces = 3, moves = 9, refusals = 7, hints = 0)
        val withAHint = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 1)

        // When / Then
        assertThat(withoutHint.isBetterThan(withAHint)).isTrue()
        assertThat(withAHint.isBetterThan(withoutHint)).isFalse()
    }

    @Test
    fun `at equal hints, fewer moves is better, even with more refusals`() {
        // Given
        val fewerMoves = PadlockOpening(pieces = 3, moves = 4, refusals = 5, hints = 1)
        val moreMoves = PadlockOpening(pieces = 3, moves = 5, refusals = 0, hints = 1)

        // When / Then
        assertThat(fewerMoves.isBetterThan(moreMoves)).isTrue()
        assertThat(moreMoves.isBetterThan(fewerMoves)).isFalse()
    }

    @Test
    fun `at equal hints and moves, fewer refusals is better`() {
        // Given
        val fewerRefusals = PadlockOpening(pieces = 3, moves = 4, refusals = 1, hints = 1)
        val moreRefusals = PadlockOpening(pieces = 3, moves = 4, refusals = 2, hints = 1)

        // When / Then
        assertThat(fewerRefusals.isBetterThan(moreRefusals)).isTrue()
        assertThat(moreRefusals.isBetterThan(fewerRefusals)).isFalse()
    }

    @Test
    fun `an equal opening is not better`() {
        // Given
        val opening = PadlockOpening(pieces = 3, moves = 4, refusals = 1, hints = 1)

        // When / Then
        assertThat(opening.isBetterThan(opening.copy())).isFalse()
    }

    @Test
    fun `openings of padlocks of different numbers of pieces are not compared`() {
        // Given
        val ofThree = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val ofFour = PadlockOpening(pieces = 4, moves = 4, refusals = 0, hints = 0)

        // When / Then
        assertThatThrownBy { ofThree.isBetterThan(ofFour) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest(name = "{0} pieces, {1} moves, {2} refusals, {3} hints")
    @CsvSource("0, 0, 0, 0", "-1, 3, 0, 0", "3, 3, -1, 0", "3, 3, 0, -1")
    fun `a padlock without pieces or a negative counter is a programming error`(
        pieces: Int,
        moves: Int,
        refusals: Int,
        hints: Int,
    ) {
        assertThatThrownBy { PadlockOpening(pieces, moves, refusals, hints) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest(name = "{0} moves")
    @ValueSource(ints = [0, 2])
    fun `a padlock is not opened in fewer moves than pieces`(moves: Int) {
        assertThatThrownBy { PadlockOpening(pieces = 3, moves = moves, refusals = 0, hints = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
