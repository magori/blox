package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.Accidental
import ch.magori.blox.core.music.ChordQuality
import ch.magori.blox.core.music.RomanNumeral

/** The seven notes, in French solfège, from C: the name of a row of the grid, without its octave. */
internal enum class NoteStep(val semitones: Int) { DO(0), RE(2), MI(4), FA(5), SOL(7), LA(9), SI(11) }

/** The name of a note: its [step], moved a semitone by its [accidental]. */
internal data class NoteName(val step: NoteStep, val accidental: Accidental = Accidental.NATURAL)

/**
 * The name of the note of [row] in [mood], spelt as the scale of the mood is: a seven-note scale takes one step per
 * degree, so that C minor reads « mi bémol », not « ré dièse »; the pentatonic scale has only natural notes.
 */
internal fun noteNameOf(
    mood: GardenMood,
    row: Int,
): NoteName {
    val semitones = mood.pitchAt(row).midi.mod(SEMITONES_PER_OCTAVE)
    val degrees = mood.scale.pitches.size
    if (degrees != NoteStep.entries.size) return NoteName(NoteStep.entries.first { it.semitones == semitones })
    val step = NoteStep.entries[(Garden.ROWS - 1 - row).mod(degrees)]
    val accidental = Accidental.entries.first { (step.semitones + it.offset).mod(SEMITONES_PER_OCTAVE) == semitones }
    return NoteName(step, accidental)
}

/** How far each accidental moves a note, in semitones. */
private val Accidental.offset: Int
    get() = when (this) {
        Accidental.FLAT -> -1
        Accidental.NATURAL -> 0
        Accidental.SHARP -> 1
    }

private const val SEMITONES_PER_OCTAVE = 12

/** The name of the scale of [mood], all on C: « Do pentatonique ». */
@StringRes
internal fun scaleNameOf(mood: GardenMood): Int = when (mood) {
    GardenMood.JOYFUL -> R.string.feature_garden_scale_major
    GardenMood.CALM -> R.string.feature_garden_scale_pentatonic
    GardenMood.MYSTERIOUS -> R.string.feature_garden_scale_harmonic_minor
    GardenMood.EPIC -> R.string.feature_garden_scale_natural_minor
}

/**
 * How TalkBack tells a chord, by its [degree] in figures, then its [accidental] if it has one, then its [quality]:
 * « 7 bémol majeur » rather than « ♭VII », read as a word.
 */
internal data class ChordTelling(
    val degree: Int,
    @param:StringRes val accidental: Int?,
    @param:StringRes val quality: Int,
)

/** How TalkBack tells [numeral]. */
internal fun chordTellingOf(numeral: RomanNumeral): ChordTelling = ChordTelling(
    degree = numeral.degree,
    accidental = when (numeral.accidental) {
        Accidental.FLAT -> R.string.feature_garden_chord_flat
        Accidental.NATURAL -> null
        Accidental.SHARP -> R.string.feature_garden_chord_sharp
    },
    quality = when (numeral.quality) {
        ChordQuality.MAJOR -> R.string.feature_garden_chord_major
        ChordQuality.MINOR -> R.string.feature_garden_chord_minor
        ChordQuality.DIMINISHED -> R.string.feature_garden_chord_diminished
    },
)
