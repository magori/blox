package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class PadlockGeneratorTest {
    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [3, 4, 5, 6])
    fun `a candidate has the number of pieces asked`(count: Int) {
        // When
        val padlock = PadlockGenerator.candidate(count, Random(1))

        // Then
        assertThat(padlock).isNotNull
        assertThat(padlock!!.solution).hasSize(count)
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [3, 4, 5, 6])
    fun `a candidate is a valid padlock whatever the random`(count: Int) {
        // Given: 300 seeds
        val seeds = 1..300L

        // When: Padlock checks its rules when it is built, so a candidate that breaks one throws
        val build = { seeds.forEach { seed -> PadlockGenerator.candidate(count, Random(seed)) } }

        // Then
        assertThatCode(build).doesNotThrowAnyException()
    }

    @Test
    fun `almost every random gives a candidate of 6 pieces`() {
        // When
        val candidates = (1..300L).mapNotNull { seed -> PadlockGenerator.candidate(6, Random(seed)) }

        // Then
        assertThat(candidates).hasSizeGreaterThanOrEqualTo(290)
    }

    @Test
    fun `over many randoms the candidates use every orientation of every type and every inner cell`() {
        // When
        val candidates = (1..300L).mapNotNull { seed -> PadlockGenerator.candidate(6, Random(seed)) }

        // Then
        val classic = PieceType.entries.flatMap { type -> ClassicRotationSystem.orientations(type).map { type to it } }
        assertThat(candidates.flatMap { it.solution }.map { it.type to it.orientation }.toSet())
            .containsExactlyInAnyOrderElementsOf(classic)
        val inner = Padlock.INSIDE.flatMap { column -> Padlock.INSIDE.map { row -> Position(column, row) } }
        assertThat(candidates.flatMap { it.hole }.toSet()).containsExactlyInAnyOrderElementsOf(inner)
    }

    @Test
    fun `the first piece of a candidate takes every placement inside the ring`() {
        // Given: every Classic piece whose cells all lie inside the ring
        val placements = PieceType.entries.flatMap { type ->
            ClassicRotationSystem.orientations(type).flatMap { orientation ->
                Padlock.INSIDE.flatMap { column ->
                    Padlock.INSIDE.map { row -> Piece(type, orientation, Position(column, row)) }
                }
            }
        }.filter { piece -> piece.cells.all { it.column in Padlock.INSIDE && it.row in Padlock.INSIDE } }

        // When
        val firsts = (1..10_000L).mapNotNull { seed -> PadlockGenerator.candidate(3, Random(seed))?.solution?.first() }

        // Then
        assertThat(firsts.toSet()).containsExactlyInAnyOrderElementsOf(placements)
    }

    @Test
    fun `a candidate that cannot place a piece is ruled out`() {
        // Given: no draw is allowed
        val attempts = 0

        // When
        val padlock = PadlockGenerator.candidate(3, Random(1), attempts)

        // Then
        assertThat(padlock).isNull()
    }

    @Test
    fun `each piece gets its own attempts`() {
        // Given: two draws per piece, a random that counts its draws (four calls per draw)
        val attempts = 2
        val randoms = (1..300L).map(::CountingRandom)

        // When
        val built = randoms.filter { PadlockGenerator.candidate(3, it, attempts) != null }

        // Then: some candidates needed more draws in all than one piece is allowed
        assertThat(built).anyMatch { it.calls / CALLS_PER_DRAW > attempts }
    }

    @Test
    fun `the same random gives the same candidate`() {
        // When
        val first = PadlockGenerator.candidate(5, Random(2026))
        val second = PadlockGenerator.candidate(5, Random(2026))

        // Then
        assertThat(first).isNotNull.isEqualTo(second)
    }

    @Test
    fun `different randoms give different candidates`() {
        // When
        val first = PadlockGenerator.candidate(5, Random(1))
        val second = PadlockGenerator.candidate(5, Random(2))

        // Then
        assertThat(first).isNotNull.isNotEqualTo(second)
    }

    /** A random that counts the numbers it gives. */
    private class CountingRandom(seed: Long) : Random() {
        private val inner = Random(seed)
        var calls = 0
            private set

        override fun nextBits(bitCount: Int): Int = inner.nextBits(bitCount).also { calls++ }
    }

    private companion object {
        /** A draw takes a type, an orientation, a column and a row. */
        const val CALLS_PER_DRAW = 4
    }
}
