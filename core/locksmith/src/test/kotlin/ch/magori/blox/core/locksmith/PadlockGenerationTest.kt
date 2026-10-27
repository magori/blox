package ch.magori.blox.core.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class PadlockGenerationTest {
    @Test
    fun `the best candidate is kept, the first one among equals`() {
        // Given: a staircase rated 156, then two padlocks rated 216, then candidates given up
        val candidates = listOf(STAIRCASE, PADLOCK, PADLOCK_SHIFTED) + List(17) { null }
        val script = candidates.iterator()

        // When
        val padlock = PadlockGenerator.generate(3, seed = 1) { script.next() }

        // Then
        assertThat(padlock).isEqualTo(PADLOCK)
    }

    @Test
    fun `the twentieth candidate is rated too`() {
        // Given: a staircase rated 156 first, the padlock rated 216 last
        val candidates = listOf(STAIRCASE) + List(18) { null } + listOf(PADLOCK)
        val script = candidates.iterator()

        // When
        val padlock = PadlockGenerator.generate(3, seed = 1) { script.next() }

        // Then
        assertThat(padlock).isEqualTo(PADLOCK)
    }

    @Test
    fun `when every candidate is given up the next seed is tried`() {
        // Given: the 20 candidates of the first seed are given up, the next ones are composed as usual
        var calls = 0
        val compose = { random: Random -> if (calls++ < 20) null else PadlockGenerator.candidate(4, random) }

        // When
        val padlock = PadlockGenerator.generate(4, seed = 7, compose = compose)

        // Then
        assertThat(padlock).isEqualTo(PadlockGenerator.generate(4, seed = 8))
    }

    @Test
    fun `when every candidate is ruled out the next seed is tried`() {
        // Given: the 20 candidates of the first seed are rectangles, the next ones are composed as usual
        var calls = 0
        val compose = { random: Random -> if (calls++ < 20) RECTANGLE else PadlockGenerator.candidate(4, random) }

        // When
        val padlock = PadlockGenerator.generate(4, seed = 7, compose = compose)

        // Then
        assertThat(padlock).isEqualTo(PadlockGenerator.generate(4, seed = 8))
    }

    @Test
    fun `when no seed gives a padlock within the bound the generator fails, 20 candidates per seed`() {
        // Given: every candidate is given up, 3 seeds at most
        var calls = 0
        val compose = { _: Random -> null.also { calls++ } }

        // Then
        assertThatIllegalStateException().isThrownBy {
            PadlockGenerator.generate(4, seed = 7, seeds = 3, compose = compose)
        }
        assertThat(calls).isEqualTo(3 * 20)
    }

    @Test
    fun `a candidate does not depend on the draws of the previous ones`() {
        // Given: the first candidate draws nothing, then 50 numbers; the second one records its first draw
        fun secondDraw(firstDraws: Int): Long {
            var calls = 0
            var second = 0L
            val compose = { random: Random ->
                when (calls++) {
                    0 -> repeat(firstDraws) { random.nextLong() }
                    1 -> second = random.nextLong()
                }
                null
            }
            runCatching { PadlockGenerator.generate(4, seed = 7, seeds = 1, compose = compose) }
            return second
        }

        // Then
        assertThat(secondDraw(firstDraws = 50)).isEqualTo(secondDraw(firstDraws = 0))
    }

    @Test
    fun `the same seed gives the same padlock`() {
        assertThat(PadlockGenerator.generate(5, seed = 2026)).isEqualTo(PadlockGenerator.generate(5, seed = 2026))
    }

    @Test
    fun `different seeds give different padlocks`() {
        assertThat(PadlockGenerator.generate(5, seed = 1)).isNotEqualTo(PadlockGenerator.generate(5, seed = 2))
    }

    @Test
    fun `negative seeds give padlocks and the seed after the largest one is the smallest one`() {
        // Given: the candidates of the largest seed are given up
        var calls = 0
        val compose = { random: Random -> if (calls++ < 20) null else PadlockGenerator.candidate(4, random) }

        // When
        val padlock = PadlockGenerator.generate(4, seed = Long.MAX_VALUE, compose = compose)

        // Then
        assertThat(padlock).isEqualTo(PadlockGenerator.generate(4, seed = Long.MIN_VALUE))
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [3, 4, 5, 6])
    fun `every padlock of 500 seeds has the pieces asked and a hole that is no rectangle`(count: Int) {
        // When
        val padlocks = (1..500L).map { seed -> PadlockGenerator.generate(count, seed) }

        // Then: Padlock checks its own rules when it is built
        assertThat(padlocks).allSatisfy { padlock ->
            assertThat(padlock.solution).hasSize(count)
            assertThat(PadlockGenerator.rating(padlock)).isNotNull
            assertThat(padlock.givenPieces).isSorted
        }
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [2, 7])
    fun `a padlock of fewer than 3 or more than 6 pieces is refused before any candidate is composed`(count: Int) {
        // Given
        var calls = 0
        val compose = { _: Random -> null.also { calls++ } }

        // Then
        assertThatIllegalArgumentException().isThrownBy { PadlockGenerator.generate(count, 1, compose = compose) }
        assertThat(calls).isZero
    }

    @Test
    fun `a generation tries one seed at least`() {
        assertThatIllegalArgumentException().isThrownBy { PadlockGenerator.generate(4, 1, seeds = 0) }
    }

    @ParameterizedTest(name = "padlock {0}: {1} pieces")
    @CsvSource("1, 3", "2, 4", "3, 5", "4, 6", "5, 6")
    fun `padlock n of a seed holds min of 6 and 2 plus n pieces`(
        level: Int,
        pieces: Int,
    ) {
        assertThat(Padlock.generate(level, seed = 2026).givenPieces).hasSize(pieces)
    }

    @Test
    fun `the same level and seed give the same padlock, another seed another one`() {
        assertThat(Padlock.generate(3, seed = 2026)).isEqualTo(Padlock.generate(3, seed = 2026))
            .isNotEqualTo(Padlock.generate(3, seed = 2027))
    }

    @Test
    fun `there is no padlock 0`() {
        assertThatIllegalArgumentException().isThrownBy { Padlock.generate(0, seed = 2026) }
    }

    @ParameterizedTest(name = "level {0}: {1} pieces")
    @CsvSource("1, 3", "2, 4", "3, 5", "4, 6", "5, 6", "20, 6")
    fun `padlock n holds min of 6 and 2 plus n pieces`(
        level: Int,
        pieces: Int,
    ) {
        assertThat(PadlockGenerator.pieceCount(level)).isEqualTo(pieces)
    }

    @ParameterizedTest(name = "level {0}")
    @ValueSource(ints = [0, -1])
    fun `there is no padlock before padlock 1`(level: Int) {
        assertThatIllegalArgumentException().isThrownBy { PadlockGenerator.pieceCount(level) }
    }

    private companion object {
        val STAIRCASE = Padlock.parse(
            """
            ##########
            #1111#####
            ##2222####
            ###3333###
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        val PADLOCK = Padlock.parse(
            """
            ##########
            #112222###
            #11333####
            ####3#####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        val PADLOCK_SHIFTED = Padlock.parse(
            """
            ##########
            ##112222##
            ##11333###
            #####3####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        val RECTANGLE = Padlock.parse(
            """
            ##########
            #1111#####
            #2222#####
            #3333#####
            #4444#####
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )
    }
}
