package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class RomanNumeralTest {
    @ParameterizedTest(name = "{0} is degree {1}, {2}")
    @CsvSource(
        "I, 1, MAJOR",
        "ii, 2, MINOR",
        "iii, 3, MINOR",
        "IV, 4, MAJOR",
        "V, 5, MAJOR",
        "vi, 6, MINOR",
        "vii°, 7, DIMINISHED",
        "i, 1, MINOR",
        "III, 3, MAJOR",
        "iv, 4, MINOR",
        "v, 5, MINOR",
        "VI, 6, MAJOR",
        "VII, 7, MAJOR",
    )
    fun `a numeral names the root degree, in capitals if major, small if minor, small with a ring if diminished`(
        name: String,
        degree: Int,
        quality: ChordQuality,
    ) {
        // When
        val numeral = RomanNumeral.parse(name)

        // Then
        assertThat(numeral).isEqualTo(RomanNumeral(degree, quality))
    }

    @ParameterizedTest(name = "{0} is degree {1}, {2}, {3}")
    @CsvSource(
        "♭VII, 7, MAJOR, FLAT",
        "♭III, 3, MAJOR, FLAT",
        "#iv, 4, MINOR, SHARP",
        "#iv°, 4, DIMINISHED, SHARP",
    )
    fun `a flat or a sharp before the numeral moves its root a semitone from the degree`(
        name: String,
        degree: Int,
        quality: ChordQuality,
        accidental: Accidental,
    ) {
        // When
        val numeral = RomanNumeral.parse(name)

        // Then
        assertThat(numeral).isEqualTo(RomanNumeral(degree, quality, accidental))
    }

    @ParameterizedTest(name = "degree {0}, {1}, {2} is named {3}")
    @CsvSource(
        "1, MAJOR, NATURAL, I",
        "6, MINOR, NATURAL, vi",
        "7, DIMINISHED, NATURAL, vii°",
        "7, MAJOR, FLAT, ♭VII",
        "2, MINOR, FLAT, ♭ii",
        "6, DIMINISHED, FLAT, ♭vi°",
        "1, MAJOR, SHARP, #I",
        "4, MINOR, SHARP, #iv",
        "4, DIMINISHED, SHARP, #iv°",
    )
    fun `a numeral is named by its accidental then its degree, capitals if major, small if minor, ringed if diminished`(
        degree: Int,
        quality: ChordQuality,
        accidental: Accidental,
        name: String,
    ) {
        // Given
        val numeral = RomanNumeral(degree, quality, accidental)

        // When / Then
        assertThat(numeral.name).isEqualTo(name)
    }

    @Test
    fun `every numeral is read back from its name`() {
        // Given
        val numerals = Accidental.entries.flatMap { accidental ->
            (1..7).flatMap { degree ->
                ChordQuality.entries.map { quality -> RomanNumeral(degree, quality, accidental) }
            }
        }

        // When / Then
        assertThat(numerals).allSatisfy { assertThat(RomanNumeral.parse(it.name)).isEqualTo(it) }
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(
        strings = [
            "", "VIII", "IIII", "Iv", "vI", "V°", "°", "1", " I", "vii°°",
            "♭♭VII", "♭", "#", "VII♭", "#♭V", "b VII", "♭ VII",
        ],
    )
    fun `a name that is not one numeral from I to VII in one case, after one flat or sharp at most, is refused`(
        name: String,
    ) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { RomanNumeral.parse(name) }
    }

    @ParameterizedTest(name = "degree {0} is refused")
    @ValueSource(ints = [0, 8])
    fun `a degree outside 1 to 7 is refused`(degree: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { RomanNumeral(degree, ChordQuality.MAJOR) }
    }
}
