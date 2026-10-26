package ch.magori.blox.core.locksmith

import ch.magori.blox.core.music.Progression
import ch.magori.blox.core.music.RomanNumeral
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PadlockChordsTest {
    @ParameterizedTest(name = "{0} pieces: {1}")
    @CsvSource("3, IV-V-I", "4, I-IV-V-I", "5, I-vi-IV-V-I", "6, I-vi-ii-IV-V-I")
    fun `the chords of a padlock follow the table of the rules`(
        pieces: Int,
        chords: String,
    ) {
        assertThat(PadlockChords.progression(pieces)).isEqualTo(Progression.parse(chords))
    }

    @Test
    fun `a padlock plays one chord per piece and ends on the perfect cadence V then I`() {
        assertThat(Padlock.PIECE_COUNTS).allSatisfy { pieces ->
            val numerals = PadlockChords.progression(pieces).numerals
            assertThat(numerals).hasSize(pieces)
            assertThat(numerals.takeLast(2)).containsExactly(RomanNumeral.parse("V"), RomanNumeral.parse("I"))
        }
    }

    @ParameterizedTest(name = "{0} pieces")
    @ValueSource(ints = [2, 7])
    fun `there are no chords for a padlock of fewer than 3 or more than 6 pieces`(pieces: Int) {
        assertThatIllegalArgumentException().isThrownBy { PadlockChords.progression(pieces) }
    }
}
