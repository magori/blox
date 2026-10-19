package ch.magori.blox.feature.composer.grid

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import ch.magori.blox.feature.composer.R

/** The name of a line as shown beside it, without its octave: « Sol », « Fa# ». */
@Composable
@ReadOnlyComposable
internal fun shownName(name: RowName): String {
    val step = stringResource(name.step.label)
    return if (name.sharp) stringResource(R.string.feature_composer_note_sharp, step) else step
}

/** The name of a line as TalkBack says it, with its octave, and out of the scale if so: « Fa dièse haut, hors gamme ». */
@Composable
@ReadOnlyComposable
internal fun spokenName(row: RowView): String {
    val step = stringResource(row.name.step.label)
    val note = if (row.name.sharp) stringResource(R.string.feature_composer_note_sharp_spoken, step) else step
    val named = stringResource(R.string.feature_composer_row, note, stringResource(row.name.register.label))
    return if (row.inScale) named else stringResource(R.string.feature_composer_row_out_of_scale, named)
}

/** A cell as TalkBack names it: its line and its beat, « Sol haut, temps 2 et demi ». */
@Composable
@ReadOnlyComposable
internal fun cellName(
    rowName: String,
    eighth: Int,
): String {
    val beat = beatOf(eighth)
    val format = if (beat.half) R.string.feature_composer_cell_half else R.string.feature_composer_cell
    return stringResource(format, rowName, beat.number)
}

/** What a cell shows, as TalkBack says it: « note tenue », « double croche : note puis silence ». */
@Composable
@ReadOnlyComposable
internal fun cellState(
    view: CellView,
    otherTrack: Boolean,
): String {
    val shown = when {
        view.isEmpty -> stringResource(R.string.feature_composer_cell_empty)

        view.isSplit -> stringResource(
            R.string.feature_composer_cell_split,
            stringResource(view.first.label),
            stringResource(view.second.label),
        )

        view.first == CellPart.ATTACK -> stringResource(R.string.feature_composer_cell_note)

        else -> stringResource(R.string.feature_composer_cell_held)
    }
    return if (otherTrack) stringResource(R.string.feature_composer_cell_other_track, shown) else shown
}

/** What touching a cell does, as TalkBack announces it. */
@Composable
@ReadOnlyComposable
internal fun actionLabel(action: CellAction): String = stringResource(
    when (action) {
        CellAction.PLACE -> R.string.feature_composer_action_place
        CellAction.ERASE -> R.string.feature_composer_action_erase
        CellAction.LENGTHEN -> R.string.feature_composer_action_lengthen
        CellAction.SHORTEN -> R.string.feature_composer_action_shorten
        CellAction.ONE_CELL -> R.string.feature_composer_action_one_cell
    },
)

@get:StringRes
private val NoteStep.label: Int
    get() = when (this) {
        NoteStep.DO -> R.string.feature_composer_note_do
        NoteStep.RE -> R.string.feature_composer_note_re
        NoteStep.MI -> R.string.feature_composer_note_mi
        NoteStep.FA -> R.string.feature_composer_note_fa
        NoteStep.SOL -> R.string.feature_composer_note_sol
        NoteStep.LA -> R.string.feature_composer_note_la
        NoteStep.SI -> R.string.feature_composer_note_si
    }

@get:StringRes
private val Register.label: Int
    get() = when (this) {
        Register.VERY_LOW -> R.string.feature_composer_register_very_low
        Register.LOW -> R.string.feature_composer_register_low
        Register.HIGH -> R.string.feature_composer_register_high
        Register.VERY_HIGH -> R.string.feature_composer_register_very_high
    }

@get:StringRes
private val CellPart.label: Int
    get() = when (this) {
        CellPart.SILENT -> R.string.feature_composer_part_silent
        CellPart.ATTACK -> R.string.feature_composer_part_note
        CellPart.HELD -> R.string.feature_composer_part_held
    }
