package ch.magori.blox.core.board.music

import ch.magori.blox.core.designsystem.text.ChordTelling
import ch.magori.blox.core.designsystem.text.SpokenAccidental
import ch.magori.blox.core.designsystem.text.SpokenChordQuality
import ch.magori.blox.core.music.Accidental
import ch.magori.blox.core.music.RomanNumeral
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** How TalkBack tells a chord, on the Locksmith and in the musical garden alike. */
class ChordTellingsTest {
    @Test
    fun `a chord is told by its degree, then its accidental, then its quality`() {
        // Given the flat seventh of the mysterious mood
        val numeral = RomanNumeral.parse("♭VII")

        // When
        val told = chordTellingOf(numeral)

        // Then
        assertThat(told).isEqualTo(ChordTelling(7, SpokenChordQuality.MAJOR, SpokenAccidental.FLAT))
        assertThat(chordTellingOf(RomanNumeral.parse("vi")).accidental).isEqualTo(SpokenAccidental.NATURAL)
    }

    @Test
    fun `a chord is read by its degree and its quality, not by its numeral, which TalkBack would read as a word`() {
        // When
        val told = listOf("I", "vi", "vii°").map { chordTellingOf(RomanNumeral.parse(it)) }

        // Then each takes the degree, 1, 6 and 7
        assertThat(told).containsExactly(
            ChordTelling(1, SpokenChordQuality.MAJOR),
            ChordTelling(6, SpokenChordQuality.MINOR),
            ChordTelling(7, SpokenChordQuality.DIMINISHED),
        )
    }

    @Test
    fun `a sharp is told sharp, as a flat flat and a natural not at all`() {
        // When
        val told = Accidental.entries.associateWith { it.spoken }

        // Then
        assertThat(told).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Accidental.FLAT to SpokenAccidental.FLAT,
                Accidental.NATURAL to SpokenAccidental.NATURAL,
                Accidental.SHARP to SpokenAccidental.SHARP,
            ),
        )
        assertThat(chordTellingOf(RomanNumeral.parse("#iv°")))
            .isEqualTo(ChordTelling(4, SpokenChordQuality.DIMINISHED, SpokenAccidental.SHARP))
    }
}
