package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class SevenBagRandomizerTest {
    @Test
    fun `the first seven draws are the seven pieces, each once`() {
        // Given a new randomizer
        val randomizer = SevenBagRandomizer(seed = 42L)

        // When it draws seven pieces
        val pieces = piecesDrawnBy(randomizer, count = 7)

        // Then
        assertThat(pieces).containsExactlyInAnyOrder(*PieceType.entries.toTypedArray())
    }

    @Test
    fun `each following group of seven draws is again the seven pieces, each once`() {
        // Given a new randomizer
        val randomizer = SevenBagRandomizer(seed = 42L)

        // When it draws ten bags
        val bags = piecesDrawnBy(randomizer, count = 7 * 10).chunked(7)

        // Then
        assertThat(bags).hasSize(10).allSatisfy { bag ->
            assertThat(bag).containsExactlyInAnyOrder(*PieceType.entries.toTypedArray())
        }
    }

    @Test
    fun `two randomizers created with the same seed draw the same sequence`() {
        val first = piecesDrawnBy(SevenBagRandomizer(seed = 42L), count = 100)
        val second = piecesDrawnBy(SevenBagRandomizer(seed = 42L), count = 100)

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `two randomizers created with different seeds draw different sequences`() {
        val first = piecesDrawnBy(SevenBagRandomizer(seed = 1L), count = 100)
        val second = piecesDrawnBy(SevenBagRandomizer(seed = 2L), count = 100)

        assertThat(first).isNotEqualTo(second)
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = [1L, 2L, 42L, 2026L])
    fun `at most twelve draws separate two appearances of the same piece`(seed: Long) {
        // Given a long sequence: the worst case is a piece first in a bag and last in the next one
        val pieces = piecesDrawnBy(SevenBagRandomizer(seed), count = 7 * 1000)

        // When the draws between two appearances of each piece are counted
        val gaps = PieceType.entries.flatMap { type ->
            pieces.indices.filter { pieces[it] == type }.zipWithNext { first, next -> next - first - 1 }
        }

        // Then
        assertThat(gaps.max()).isLessThanOrEqualTo(12)
    }

    @Test
    fun `the previous piece is ignored, unlike in the Classic mode`() {
        // Given two randomizers with the same seed
        val told = SevenBagRandomizer(seed = 42L)
        val misled = SevenBagRandomizer(seed = 42L)

        // When one is told the true previous piece and the other is always told an O
        val drawnFromTruth = piecesDrawnBy(told, count = 100)
        val drawnFromO = List(100) { misled.next(previous = PieceType.O) }

        // Then they draw the same pieces
        assertThat(drawnFromO).isEqualTo(drawnFromTruth)
    }

    @Test
    fun `a source that swaps no piece gives the bag in its base order I J L O S T Z, not in the order of the enum`() {
        // Given a randomizer whose source keeps every piece in place while shuffling
        val randomizer = SevenBagRandomizer(NoSwapRandom())

        // When it draws the first bag
        val bag = piecesDrawnBy(randomizer, count = 7)

        // Then
        assertThat(bag).containsExactly(
            PieceType.I,
            PieceType.J,
            PieceType.L,
            PieceType.O,
            PieceType.S,
            PieceType.T,
            PieceType.Z,
        )
    }

    @Test
    fun `the seed 42 gives these first fourteen pieces, an alarm if Kotlin changes Random or shuffled`() {
        // When a randomizer with the seed 42 draws two bags
        val pieces = piecesDrawnBy(SevenBagRandomizer(seed = 42L), count = 14)

        // Then they are the pieces recorded with Kotlin 2.4.21
        assertThat(pieces.chunked(7) { bag -> bag.joinToString("") }).containsExactly("LTSOJIZ", "OTJISZL")
    }

    /** When: [randomizer] draws [count] pieces, each from the one before. */
    private fun piecesDrawnBy(
        randomizer: Randomizer,
        count: Int,
    ): List<PieceType> = generateSequence(randomizer.next(previous = null)) { randomizer.next(previous = it) }
        .take(count)
        .toList()

    /**
     * A random source that, asked for a draw in `0 until n`, always gives `n - 1`: in the shuffle of the standard
     * library (Fisher-Yates from the last index down), each piece is then swapped with itself.
     */
    private class NoSwapRandom : Random() {
        override fun nextBits(bitCount: Int): Int = error("Only nextInt(until) is scripted")

        override fun nextInt(until: Int): Int = until - 1
    }
}
