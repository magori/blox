package ch.magori.blox.core.designsystem.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.R

/** The seven notes, in French solfège, from C: the name of a note, without its octave. */
enum class NoteStep { DO, RE, MI, FA, SOL, LA, SI }

/** What moves a note, or the root of a chord, a semitone as TalkBack says it: « bémol », nothing, « dièse ». */
enum class SpokenAccidental { FLAT, NATURAL, SHARP }

/** The quality of a chord as TalkBack says it: « majeur », « mineur », « diminué ». */
enum class SpokenChordQuality { MAJOR, MINOR, DIMINISHED }

/**
 * How TalkBack tells a chord, by its [degree] in figures, then its [accidental] if it has one, then its [quality]:
 * « 7 bémol majeur » rather than « ♭VII », read as a word.
 */
data class ChordTelling(
    val degree: Int,
    val quality: SpokenChordQuality,
    val accidental: SpokenAccidental = SpokenAccidental.NATURAL,
)

/** The words of a chord of this quality, after its root: « %1$s majeur ». */
@get:StringRes
internal val SpokenChordQuality.text: Int
    get() = when (this) {
        SpokenChordQuality.MAJOR -> R.string.core_designsystem_chord_major
        SpokenChordQuality.MINOR -> R.string.core_designsystem_chord_minor
        SpokenChordQuality.DIMINISHED -> R.string.core_designsystem_chord_diminished
    }

/** The words of a note, or of the root of a chord, moved by this accidental, after its name: « %1$s bémol ». */
@get:StringRes
internal val SpokenAccidental.text: Int?
    get() = when (this) {
        SpokenAccidental.FLAT -> R.string.core_designsystem_flat
        SpokenAccidental.NATURAL -> null
        SpokenAccidental.SHARP -> R.string.core_designsystem_sharp
    }

/** The name of this note, in lower case: « ré ». */
@get:StringRes
internal val NoteStep.text: Int
    get() = when (this) {
        NoteStep.DO -> R.string.core_designsystem_note_do
        NoteStep.RE -> R.string.core_designsystem_note_re
        NoteStep.MI -> R.string.core_designsystem_note_mi
        NoteStep.FA -> R.string.core_designsystem_note_fa
        NoteStep.SOL -> R.string.core_designsystem_note_sol
        NoteStep.LA -> R.string.core_designsystem_note_la
        NoteStep.SI -> R.string.core_designsystem_note_si
    }

/** A note as TalkBack says it, without its octave, in lower case: « mi », « mi bémol », « fa dièse ». */
@Composable
@ReadOnlyComposable
fun noteText(
    step: NoteStep,
    accidental: SpokenAccidental = SpokenAccidental.NATURAL,
): String = accidentalText(stringResource(step.text), accidental)

/** A chord as TalkBack says it: « 6 mineur », « 7 bémol majeur ». */
@Composable
@ReadOnlyComposable
fun chordText(telling: ChordTelling): String =
    stringResource(telling.quality.text, accidentalText(telling.degree.toString(), telling.accidental))

/** Chords as TalkBack says them, one after the other: « 5 majeur, 1 majeur ». */
@Composable
@ReadOnlyComposable
fun chordsText(tellings: List<ChordTelling>): String {
    val separator = stringResource(R.string.core_designsystem_chords_separator)
    return tellings.map { chordText(it) }.joinToString(separator)
}

/** [name] moved by [accidental]: « mi bémol », « 7 bémol », or [name] alone when natural. */
@Composable
@ReadOnlyComposable
private fun accidentalText(
    name: String,
    accidental: SpokenAccidental,
): String = accidental.text?.let { stringResource(it, name) } ?: name
