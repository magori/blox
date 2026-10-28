package ch.magori.blox.core.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PadlockProfileTest {
    @ParameterizedTest(name = "padlock {0}: {1} pieces, {2}, {3}, {4}")
    @CsvSource(
        "1, 3, WITHOUT_S_Z, SINUOUS, FREE",
        "2, 4, WITHOUT_S_Z, SINUOUS, FREE",
        "3, 5, ALL, SINUOUS, FREE",
        "4, 6, ALL, SINUOUS, FREE",
        "6, 6, ALL, SINUOUS, FREE",
        "7, 6, ALL, MIXED, FEW",
        "12, 6, ALL, MIXED, FEW",
        "13, 7, ALL, MIXED, FEW",
        "20, 7, ALL, MIXED, FEW",
        "21, 8, ALL, COMPACT, FEW",
        "30, 8, ALL, COMPACT, FEW",
    )
    fun `the padlocks 1 to 30 follow the table of the tiers`(
        level: Int,
        pieces: Int,
        types: String,
        shape: String,
        solutions: String,
    ) {
        assertThat(PadlockProfile.of(level)).isEqualTo(profile(pieces, types, shape, solutions))
    }

    @ParameterizedTest(name = "padlock {0}: {1} pieces, {2}, {3}, {4}")
    @CsvSource(
        "31, 8, ALL, COMPACT, UNIQUE",
        "32, 8, ALL, SINUOUS, FREE",
        "33, 8, S_Z_T, MIXED, FEW",
        "34, 8, ALL, COMPACT, FEW",
        "35, 7, ONE_OF_EACH, MIXED, FEW",
        "36, 8, ALL, COMPACT, UNIQUE",
        "100, 7, ONE_OF_EACH, MIXED, FEW",
        "1004, 8, ALL, COMPACT, FEW",
    )
    fun `beyond padlock 30 the padlocks turn on a cycle of 5`(
        level: Int,
        pieces: Int,
        types: String,
        shape: String,
        solutions: String,
    ) {
        assertThat(PadlockProfile.of(level)).isEqualTo(profile(pieces, types, shape, solutions))
    }

    @Test
    fun `a profile of one of each holds 7 pieces at most`() {
        assertThatIllegalArgumentException().isThrownBy {
            PadlockProfile(8, PieceSet.ONE_OF_EACH, HoleShape.MIXED, SolutionPreference.FEW)
        }
    }

    @ParameterizedTest(name = "level {0}")
    @ValueSource(ints = [0, -1])
    fun `there is no padlock before padlock 1`(level: Int) {
        assertThatIllegalArgumentException().isThrownBy { PadlockProfile.of(level) }
    }
}

/** The profile of [pieces] pieces whose types, shape and preference of solutions are given by name. */
private fun profile(
    pieces: Int,
    types: String,
    shape: String,
    solutions: String,
) = PadlockProfile(pieces, PieceSet.valueOf(types), HoleShape.valueOf(shape), SolutionPreference.valueOf(solutions))
