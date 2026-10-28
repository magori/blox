package ch.magori.blox.core.board.music

import ch.magori.blox.core.designsystem.text.ChordTelling
import ch.magori.blox.core.designsystem.text.SpokenAccidental
import ch.magori.blox.core.designsystem.text.SpokenChordQuality
import ch.magori.blox.core.music.Accidental
import ch.magori.blox.core.music.ChordQuality
import ch.magori.blox.core.music.RomanNumeral

/** How TalkBack tells [numeral], the same on every screen that reads chords (the Locksmith, the musical garden). */
fun chordTellingOf(numeral: RomanNumeral): ChordTelling = ChordTelling(
    degree = numeral.degree,
    quality = when (numeral.quality) {
        ChordQuality.MAJOR -> SpokenChordQuality.MAJOR
        ChordQuality.MINOR -> SpokenChordQuality.MINOR
        ChordQuality.DIMINISHED -> SpokenChordQuality.DIMINISHED
    },
    accidental = numeral.accidental.spoken,
)

/** How TalkBack says [this] accidental, of a chord or of a note. */
val Accidental.spoken: SpokenAccidental
    get() = when (this) {
        Accidental.FLAT -> SpokenAccidental.FLAT
        Accidental.NATURAL -> SpokenAccidental.NATURAL
        Accidental.SHARP -> SpokenAccidental.SHARP
    }
