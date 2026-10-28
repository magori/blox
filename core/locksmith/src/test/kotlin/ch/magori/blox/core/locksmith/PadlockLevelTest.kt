package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.rules.StableRandom
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class PadlockLevelTest {
    @Test
    fun `the candidates of padlock n draw on StableRandom, seeded by the numbers of StableRandom of n`() {
        // Given: each candidate records its first number, and is given up
        val firsts = mutableListOf<Long>()
        val compose = { _: PadlockProfile, random: Random -> null.also { firsts += random.nextLong() } }

        // When
        runCatching { PadlockGenerator.ofLevel(27, tries = 1, compose = compose) }

        // Then
        val master = StableRandom(27)
        assertThat(firsts).isEqualTo(List(20) { StableRandom(master.nextLong()).nextLong() })
    }

    @Test
    fun `when every candidate is given up, try k draws on the pair n, k mixed by SplitMix, not on n + k`() {
        // Given: the 20 candidates of the first try are given up, the first one of the second try records its number
        var calls = 0
        var first = 0L
        val compose = { _: PadlockProfile, random: Random ->
            if (calls++ == 20) first = random.nextLong()
            null
        }

        // When
        runCatching { PadlockGenerator.ofLevel(27, tries = 2, compose = compose) }

        // Then: the master of try 1 is seeded by the first number of StableRandom(27 shl 32 or 1)
        val master = StableRandom(StableRandom((27L shl 32) or 1L).nextLong())
        assertThat(first).isEqualTo(StableRandom(master.nextLong()).nextLong())
            .isNotEqualTo(StableRandom(StableRandom(28).nextLong()).nextLong())
    }

    @Test
    fun `the seeds of the tries of neighbouring padlocks never meet`() {
        // Given: try 1 of padlock 27 against the first try of padlock 28 and try 2 of padlock 26, and their neighbours
        val pairs = (1..60).flatMap { level -> (0..4).map { tried -> level to tried } }

        // When
        val seeds = pairs.map { (level, tried) -> PadlockGenerator.seedOf(level, tried) }

        // Then
        assertThat(PadlockGenerator.seedOf(27, 1))
            .isNotEqualTo(PadlockGenerator.seedOf(28, 0))
            .isNotEqualTo(PadlockGenerator.seedOf(26, 2))
        assertThat(seeds).doesNotHaveDuplicates()
    }

    @Test
    fun `a padlock takes one try at least`() {
        assertThatIllegalArgumentException().isThrownBy { PadlockGenerator.ofLevel(27, tries = 0) }
    }

    @Test
    fun `when no try gives a padlock within the bound the generator fails, 20 candidates per try`() {
        // Given: every candidate is given up, 3 tries at most
        var calls = 0
        val compose = { _: PadlockProfile, _: Random -> null.also { calls++ } }

        // Then
        assertThatIllegalStateException().isThrownBy { PadlockGenerator.ofLevel(27, tries = 3, compose = compose) }
        assertThat(calls).isEqualTo(3 * 20)
    }

    @ParameterizedTest(name = "padlock {0}")
    @ValueSource(ints = [1, 2, 7, 13, 21, 31, 33, 35])
    fun `padlock n holds the pieces of its profile`(level: Int) {
        // Given
        val profile = PadlockProfile.of(level)

        // When
        val padlock = Padlock.ofLevel(level)

        // Then
        assertThat(padlock.solution).hasSize(profile.pieceCount)
        assertThat(padlock.givenPieces).allMatch { it in profile.types.drawable(emptyList()) }
    }

    @Test
    fun `a padlock is rated for the shape and the preference of its profile`() {
        // Given: a winding hole of 2 solutions, rated 188 as sinuous, -62 as compact of a unique solution; a compact
        // hole of 1 solution, rated 180 as sinuous, -9 as compact of a unique solution
        val candidates = { listOf(TWO_SOLUTIONS, ONE_SOLUTION).iterator() }
        val compose = { script: Iterator<Padlock> -> { _: PadlockProfile, _: Random -> script.nextOrNull() } }

        // When: padlock 1 is sinuous, its solutions free; padlock 31 compact, a unique solution preferred
        val first = PadlockGenerator.ofLevel(1, compose = compose(candidates()))
        val thirtyFirst = PadlockGenerator.ofLevel(31, compose = compose(candidates()))

        // Then
        assertThat(first).isEqualTo(TWO_SOLUTIONS)
        assertThat(thirtyFirst).isEqualTo(ONE_SOLUTION)
    }

    private companion object {
        val TWO_SOLUTIONS = Padlock.parse(
            """
            ##########
            #1111#####
            #2222#####
            #3333#####
            #4444#####
            ##555#####
            ###5######
            ##########
            ##########
            ##########
            """,
        )

        val ONE_SOLUTION = Padlock.parse(
            """
            ##########
            #1111#####
            ##2222####
            ####33####
            ####33####
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
    }
}
