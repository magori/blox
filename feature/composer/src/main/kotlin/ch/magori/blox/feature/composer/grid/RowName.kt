package ch.magori.blox.feature.composer.grid

import ch.magori.blox.core.designsystem.text.NoteStep
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.PitchRange

/**
 * The octave of a line within the range of its track, which spans two octaves and the note above: TalkBack tells
 * « Sol bas » from « Sol haut », and the top « Do très haut ». A note opened out of the range is very low or very high.
 */
internal enum class Register { VERY_LOW, LOW, HIGH, VERY_HIGH }

/** The name of a line of the grid: its [step], raised by a [sharp] or not, in its [register]. */
internal data class RowName(val step: NoteStep, val sharp: Boolean, val register: Register)

/** The name of the line of [pitch] on the grid of a track of [range], written with sharps as the pitches are. */
internal fun rowName(
    pitch: Pitch,
    range: PitchRange,
): RowName {
    val (step, sharp) = NAMES[pitch.midi.mod(SEMITONES_PER_OCTAVE)]
    val octave = (pitch.midi - range.lowest.midi).floorDiv(SEMITONES_PER_OCTAVE)
    val register = when {
        octave < 0 -> Register.VERY_LOW
        octave == 0 -> Register.LOW
        octave == 1 -> Register.HIGH
        else -> Register.VERY_HIGH
    }
    return RowName(step, sharp, register)
}

private const val SEMITONES_PER_OCTAVE = 12

/** The name of each pitch class, from C. */
private val NAMES: List<Pair<NoteStep, Boolean>> = NoteStep.entries.flatMap { step ->
    if (step == NoteStep.MI || step == NoteStep.SI) listOf(step to false) else listOf(step to false, step to true)
}
