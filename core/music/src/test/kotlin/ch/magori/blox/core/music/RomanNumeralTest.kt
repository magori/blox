package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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
