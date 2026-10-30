package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import ch.magori.blox.core.designsystem.text.NoteStep
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.Accidental

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

/** How far each note lies above C, in semitones. */
private val NoteStep.semitones: Int
    get() = when (this) {
        NoteStep.DO -> 0
        NoteStep.RE -> 2
        NoteStep.MI -> 4
        NoteStep.FA -> 5
        NoteStep.SOL -> 7
        NoteStep.LA -> 9
        NoteStep.SI -> 11
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
