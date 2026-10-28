package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.board.garden.GardenCell
import ch.magori.blox.core.board.music.chordTellingOf
import ch.magori.blox.core.board.music.spoken
import ch.magori.blox.core.designsystem.text.chordsText
import ch.magori.blox.core.designsystem.text.noteText
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.GardenMood

/** The name of [mood] on its segment. */
@get:StringRes
internal val GardenMood.label: Int
    get() = when (this) {
        GardenMood.JOYFUL -> R.string.feature_garden_mood_joyful
        GardenMood.CALM -> R.string.feature_garden_mood_calm
        GardenMood.MYSTERIOUS -> R.string.feature_garden_mood_mysterious
        GardenMood.EPIC -> R.string.feature_garden_mood_epic
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
    val name = noteNameOf(mood, position.row)
    val note = noteText(name.step, name.accidental.spoken)
    return stringResource(R.string.feature_garden_cell, position.column + 1, note, what)
}

/** What TalkBack reads of the band of the chords of [mood], in one: « Accords : 1 majeur, 6 mineur, … ». */
@Composable
@ReadOnlyComposable
internal fun moodChordsText(mood: GardenMood): String =
    stringResource(R.string.feature_garden_chords, chordsText(mood.progression.numerals.map(::chordTellingOf)))
