package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HarmonyTest {
    @Test
    fun `a scale without a progression to play on it is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Harmony(ScaleType.MAJOR, emptyList()) }
    }

    @ParameterizedTest(name = "{1} on {0}")
    @CsvSource("MAJOR, I-II-V-I", "NATURAL_MINOR, i-iv-V-i", "DORIAN, i-iv", "MIXOLYDIAN, I-V-vi-IV")
    fun `a progression with a chord out of the scale, though written without an accidental, is refused`(
        scaleType: ScaleType,
        progression: String,
    ) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            Harmony(scaleType, listOf(Progression.parse(progression)))
        }
    }

    @ParameterizedTest(name = "{1} on {0}")
    @CsvSource("HARMONIC_MINOR, i-♭VII-VI-V", "MAJOR, I-♭VII-IV-I")
    fun `a numeral with an accidental borrows a chord whose root is out of the scale`(
        scaleType: ScaleType,
        progression: String,
    ) {
        // Given
        val progressions = listOf(Progression.parse(progression))

        // When / Then
        assertThatCode { Harmony(scaleType, progressions) }.doesNotThrowAnyException()
    }

    @Test
    fun `a progression on a pentatonic scale may hold the chords of the major scale, not only its five notes`() {
        // Given
        val progressions = listOf(Progression.parse("I-vi-IV-V"))

        // When / Then
        assertThatCode { Harmony(ScaleType.MAJOR_PENTATONIC, progressions) }.doesNotThrowAnyException()
    }

    @ParameterizedTest(name = "{1} on {0}")
    @CsvSource("MIXOLYDIAN, I-♭VII-IV-I", "NATURAL_MINOR, i-♭VI", "MAJOR, I-#III")
    fun `an accidental that lands the root on another degree of the scale is refused, as it would name that degree`(
        scaleType: ScaleType,
        progression: String,
    ) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            Harmony(scaleType, listOf(Progression.parse(progression)))
        }
    }
}
