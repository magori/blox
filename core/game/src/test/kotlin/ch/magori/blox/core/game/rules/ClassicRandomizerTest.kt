package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random

class ClassicRandomizerTest {
    @ParameterizedTest(name = "previous {0}, draw {1} -> {2}")
    @CsvSource("O, 0, T", "O, 6, I", "T, 3, O")
    fun `a valid first draw different from the previous piece is kept`(
        previous: PieceType,
        draw: Int,
        expected: PieceType,
    ) {
        assertThat(nextPiece(previous, draw)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "previous {0}, draws 7 then {1} -> {2}")
    @CsvSource("O, 2, Z", "T, 1, J", "I, 5, L", "O, 4, S")
    fun `a first draw of 7 is followed by a second draw that is kept`(
        previous: PieceType,
        secondDraw: Int,
        expected: PieceType,
    ) {
        assertThat(nextPiece(previous, 7, secondDraw)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "previous {0}, draws {1} then {2} -> {3}")
    @CsvSource("T, 0, 3, O", "I, 6, 0, T", "O, 3, 3, O")
    fun `a first draw equal to the previous piece is followed by a second draw that is kept even if it repeats`(
        previous: PieceType,
        firstDraw: Int,
        secondDraw: Int,
        expected: PieceType,
    ) {
        assertThat(nextPiece(previous, firstDraw, secondDraw)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "no previous piece, draws {0} -> {1}")
    @CsvSource("0, T", "1, J", "2, Z", "3, O", "4, S", "5, L", "6, I")
    fun `the first draw maps to the pieces in the ROM order T J Z O S L I`(
        draw: Int,
        expected: PieceType,
    ) {
        assertThat(nextPiece(previous = null, draw)).isEqualTo(expected)
    }

    @Test
    fun `for the first piece of the game a first draw of 7 is followed by a second draw`() {
        assertThat(nextPiece(previous = null, 7, 2)).isEqualTo(PieceType.Z)
    }

    @Test
    fun `the first draw is made among 8 values and the second among 7`() {
        val random = ScriptedRandom(7, 2)

        nextPiece(previous = PieceType.O, random)

        assertThat(random.bounds).containsExactly(8, 7)
    }

    @Test
    fun `two randomizers created with the same seed draw the same sequence`() {
        val first = piecesDrawnBy(ClassicRandomizer(seed = 42L))
        val second = piecesDrawnBy(ClassicRandomizer(seed = 42L))

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `two randomizers created with different seeds draw different sequences`() {
        val first = piecesDrawnBy(ClassicRandomizer(seed = 1L))
        val second = piecesDrawnBy(ClassicRandomizer(seed = 2L))

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `the first 20 pieces of seed 20261026 are fixed`() {
        val pieces = piecesDrawnBy(ClassicRandomizer.stable(seed = 20261026L)).take(20)

        // A contract between versions of the application (ADR 0026): changing it calls for a new ADR.
        assertThat(pieces.joinToString(" ")).isEqualTo("Z T I Z O Z J L S Z T I J L Z O L Z I O")
    }

    /** When: the randomizer draws the piece that follows [previous], the source giving [draws] in turn. */
    private fun nextPiece(
        previous: PieceType?,
        vararg draws: Int,
    ): PieceType = nextPiece(previous, ScriptedRandom(*draws))

    /** When: the randomizer draws the piece that follows [previous] from [random]. */
    private fun nextPiece(
        previous: PieceType?,
        random: Random,
    ): PieceType = ClassicRandomizer(random).next(previous)

    /** When: [randomizer] draws the first 100 pieces of a game, each from the one before. */
    private fun piecesDrawnBy(randomizer: Randomizer): List<PieceType> =
        generateSequence(randomizer.next(previous = null)) { randomizer.next(previous = it) }.take(100).toList()

    /** A random source that gives scripted values, in order, and records the bounds it was asked for. */
    private class ScriptedRandom(vararg draws: Int) : Random() {
        private val remaining = ArrayDeque(draws.toList())
        val bounds = mutableListOf<Int>()

        override fun nextBits(bitCount: Int): Int = error("Only nextInt(until) is scripted")

        override fun nextInt(until: Int): Int {
            bounds += until
            val value = remaining.removeFirstOrNull() ?: error("No scripted draw left (bounds so far: $bounds)")
            require(value in 0 until until) { "Scripted draw $value is outside 0 until $until" }
            return value
        }
    }
}
