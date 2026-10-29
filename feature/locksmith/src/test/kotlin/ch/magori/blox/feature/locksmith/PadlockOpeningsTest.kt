package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.locksmith.PadlockResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

/**
 * The adapter between the opening of a game, `PadlockResult` of `core:locksmith`, and the opening the progress keeps,
 * `PadlockOpening` of `core:data`, which does not see the domain (ADR 0025, amendment P7.28): two types of the same
 * rules, kept in agreement here, the one module that sees both.
 */
class PadlockOpeningsTest {
    @Test
    fun `a result becomes the opening of the same counters, and back, without loss`() {
        // Given
        val result = PadlockResult(pieces = 5, moves = 7, refusals = 2, hints = 1)

        // When
        val opening = result.toOpening()

        // Then
        assertThat(opening).isEqualTo(PadlockOpening(pieces = 5, moves = 7, refusals = 2, hints = 1))
        assertThat(opening.toResult()).isEqualTo(result)
    }

    @Test
    fun `a result and an opening refuse the same counters, with the same failure`() {
        // Given counters on each side of each rule: no piece, fewer moves than pieces, a negative counter
        val counters = listOf(
            listOf(1, 1, 0, 0),
            listOf(0, 0, 0, 0),
            listOf(-1, 3, 0, 0),
            listOf(3, 3, 0, 0),
            listOf(3, 2, 0, 0),
            listOf(3, 0, 0, 0),
            listOf(3, 4, -1, 0),
            listOf(3, 4, 0, -1),
            listOf(3, 4, -1, -1),
            listOf(3, 4, 2, 5),
        )

        // When
        val failures = counters.map { (pieces, moves, refusals, hints) ->
            val result = catchThrowable { PadlockResult(pieces, moves, refusals, hints) }
            val opening = catchThrowable { PadlockOpening(pieces, moves, refusals, hints) }
            result?.javaClass?.name to opening?.javaClass?.name
        }

        // Then each side fails with an IllegalArgumentException, or neither does
        assertThat(failures.map { it.first }).containsExactly(
            null,
            IAE,
            IAE,
            null,
            IAE,
            IAE,
            IAE,
            IAE,
            IAE,
            null,
        )
        failures.forEach { (result, opening) -> assertThat(opening).isEqualTo(result) }
    }

    @Test
    fun `a result and an opening tell the better of two the same way, each criterion alone and against the others`() {
        // Given pairs of openings of 4 pieces, (moves, refusals, hints), that differ by one criterion, or trade one
        // against another, or not at all
        val pairs = listOf(
            // Hints alone
            Triple(4, 0, 0) to Triple(4, 0, 1),
            // Moves alone
            Triple(5, 0, 0) to Triple(4, 0, 0),
            // Refusals alone
            Triple(4, 1, 0) to Triple(4, 0, 0),
            // Hints over moves and refusals
            Triple(9, 9, 0) to Triple(4, 0, 1),
            // Moves over refusals
            Triple(4, 9, 0) to Triple(5, 0, 0),
            // Equal
            Triple(6, 2, 1) to Triple(6, 2, 1),
        )

        // When each pair is compared both ways, as results and as openings
        val told = pairs.flatMap { (a, b) -> listOf(a to b, b to a) }.map { (a, b) ->
            val asResults = result(a).isBetterThan(result(b))
            val asOpenings = result(a).toOpening().isBetterThan(result(b).toOpening())
            asResults to asOpenings
        }

        // Then
        assertThat(told.map { it.first }).containsExactly(
            true,
            false,
            false,
            true,
            false,
            true,
            true,
            false,
            true,
            false,
            false,
            false,
        )
        told.forEach { (asResults, asOpenings) -> assertThat(asOpenings).isEqualTo(asResults) }
    }

    @Test
    fun `a result and an opening both refuse to compare openings of different numbers of pieces`() {
        // Given
        val three = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val four = PadlockResult(pieces = 4, moves = 4, refusals = 0, hints = 0)

        // When
        val asResults = catchThrowable { three.isBetterThan(four) }
        val asOpenings = catchThrowable { three.toOpening().isBetterThan(four.toOpening()) }

        // Then
        assertThat(asResults).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(asOpenings).isInstanceOf(IllegalArgumentException::class.java)
    }

    /** The result of a padlock of 4 pieces of these (moves, refusals, hints). */
    private fun result(counters: Triple<Int, Int, Int>) =
        PadlockResult(pieces = 4, moves = counters.first, refusals = counters.second, hints = counters.third)

    private companion object {
        val IAE: String = IllegalArgumentException::class.java.name
    }
}
