package ch.magori.blox.core.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PadlockTierTest {
    @ParameterizedTest(name = "padlock {0}: {1}")
    @CsvSource(
        "1, APPRENTICE",
        "3, APPRENTICE",
        "4, JOURNEYMAN",
        "12, JOURNEYMAN",
        "13, CRAFTSMAN",
        "20, CRAFTSMAN",
        "21, MASTER",
        "30, MASTER",
        "31, MASTER",
        "1004, MASTER",
    )
    fun `the padlocks follow the tiers of the table, the cycle being of masters`(
        level: Int,
        tier: PadlockTier,
    ) {
        assertThat(PadlockTier.of(level)).isEqualTo(tier)
    }

    @Test
    fun `the pieces of a tier are those of its padlocks, from the fewest to the most`() {
        // Given the padlocks 1 to 200, the tiers and the cycle beyond 30 covered many times
        val pieces = (1..200).groupBy(PadlockTier::of) { PadlockProfile.of(it).pieceCount }

        // Then
        assertThat(pieces.mapValues { (_, counts) -> counts.min()..counts.max() })
            .isEqualTo(PadlockTier.entries.associateWith(PadlockTier::pieces))
    }

    @Test
    fun `there is no tier before padlock 1`() {
        assertThatIllegalArgumentException().isThrownBy { PadlockTier.of(0) }
    }
}
