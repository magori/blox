package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.board.garden.GardenCell
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.Accidental
import ch.magori.blox.core.music.RomanNumeral

/** The name of [mood] on its segment. */
@get:StringRes
internal val GardenMood.label: Int
    get() = when (this) {
        GardenMood.JOYFUL -> R.string.feature_garden_mood_joyful
        GardenMood.CALM -> R.string.feature_garden_mood_calm
        GardenMood.MYSTERIOUS -> R.string.feature_garden_mood_mysterious
        GardenMood.EPIC -> R.string.feature_garden_mood_epic
    }

/** A note as TalkBack says it, without its octave: « mi », « mi bémol ». */
@Composable
@ReadOnlyComposable
internal fun noteText(name: NoteName): String {
    val step = stringResource(name.step.label)
    return when (name.accidental) {
        Accidental.FLAT -> stringResource(R.string.feature_garden_note_flat, step)
        Accidental.NATURAL -> step
        Accidental.SHARP -> stringResource(R.string.feature_garden_note_sharp, step)
    }
}

/**
 * What TalkBack reads of [cell], at [position] of a garden heard in [mood]: its eighth, from 1, its note, then what it
 * holds: « Croche 5, mi : libre ».
 */
@Composable
@ReadOnlyComposable
internal fun cellText(
    cell: GardenCell,
    position: Position,
    mood: GardenMood,
): String {
    val what = when (cell) {
        GardenCell.Free -> stringResource(R.string.feature_garden_cell_free)

        is GardenCell.Note -> stringResource(
            if (cell.inChord) R.string.feature_garden_cell_note_chord else R.string.feature_garden_cell_note,
            cell.type.name,
        )

        is GardenCell.Preview -> stringResource(
            if (cell.fits) R.string.feature_garden_cell_preview else R.string.feature_garden_cell_preview_refused,
            cell.type.name,
        )
    }
    val note = noteText(noteNameOf(mood, position.row))
    return stringResource(R.string.feature_garden_cell, position.column + 1, note, what)
}

/** What TalkBack reads of the band of the chords of [mood], in one: « Accords : 1 majeur, 6 mineur, … ». */
@Composable
@ReadOnlyComposable
internal fun chordsText(mood: GardenMood): String {
    val separator = stringResource(R.string.feature_garden_chords_separator)
    val chords = mood.progression.numerals.map { chordText(it) }.joinToString(separator)
    return stringResource(R.string.feature_garden_chords, chords)
}

/** A chord as TalkBack says it: « 7 bémol majeur ». */
@Composable
@ReadOnlyComposable
private fun chordText(numeral: RomanNumeral): String {
    val telling = chordTellingOf(numeral)
    val degree = telling.degree.toString()
    val root = telling.accidental?.let { stringResource(it, degree) } ?: degree
    return stringResource(telling.quality, root)
}

@get:StringRes
private val NoteStep.label: Int
    get() = when (this) {
        NoteStep.DO -> R.string.feature_garden_note_do
        NoteStep.RE -> R.string.feature_garden_note_re
        NoteStep.MI -> R.string.feature_garden_note_mi
        NoteStep.FA -> R.string.feature_garden_note_fa
        NoteStep.SOL -> R.string.feature_garden_note_sol
        NoteStep.LA -> R.string.feature_garden_note_la
        NoteStep.SI -> R.string.feature_garden_note_si
    }
