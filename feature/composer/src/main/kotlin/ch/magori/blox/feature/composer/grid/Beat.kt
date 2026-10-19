package ch.magori.blox.feature.composer.grid

import ch.magori.blox.feature.composer.model.CELLS_PER_BAR

/** A beat of the bar, [number] from 1 to 4, on its start or on its second [half]: TalkBack says « temps 2 et demi ». */
internal data class Beat(val number: Int, val half: Boolean)

/** The beat of the [eighth] cell of a bar, counted from 0. */
internal fun beatOf(eighth: Int): Beat = Beat(number = eighth / CELLS_PER_BEAT + 1, half = eighth % CELLS_PER_BEAT == 1)

/** Beats in a bar of 4/4. */
internal const val BEATS = 4

/** Cells in a beat: a cell is an eighth note, a beat a quarter note. */
internal const val CELLS_PER_BEAT = CELLS_PER_BAR / BEATS
