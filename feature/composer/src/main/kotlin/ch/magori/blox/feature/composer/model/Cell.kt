package ch.magori.blox.feature.composer.model

/** Bars of a music of the editor. */
internal const val BARS: Int = 8

/** Cells of a bar of four beats: one eighth note each, two per beat. */
internal const val CELLS_PER_BAR: Int = 8

/** Sixteenth notes in a cell: the grid keeps the sixteenth notes of a generated music, two to a cell. */
internal const val SIXTEENTHS_PER_CELL: Int = 2

/** Beats in a sixteenth note: the beat is a quarter note. */
internal const val BEATS_PER_SIXTEENTH: Double = 0.25

/** Sixteenth notes in a bar. */
internal const val SIXTEENTHS_PER_BAR: Int = CELLS_PER_BAR * SIXTEENTHS_PER_CELL

/** Sixteenth notes in the music. */
internal const val SIXTEENTHS: Int = BARS * SIXTEENTHS_PER_BAR

/** The first sixteenth note of [bar], counted from 0, from the start of the music. */
internal fun firstSixteenthOf(bar: Int): Int = bar * SIXTEENTHS_PER_BAR

/** The cell of the [eighth] eighth note of the music, counted from 0 from its start. */
internal fun cellAt(eighth: Int): Cell = Cell(bar = eighth / CELLS_PER_BAR, eighth = eighth % CELLS_PER_BAR)

/** A cell of the grid: the [eighth] eighth note of [bar], both counted from 0. */
internal data class Cell(val bar: Int, val eighth: Int) {
    init {
        requireBar(bar)
        require(eighth in 0 until CELLS_PER_BAR) { "A cell goes from 0 to ${CELLS_PER_BAR - 1}, was $eighth" }
    }

    /** The first sixteenth note of the cell, counted from the start of the music. */
    val start: Int get() = firstSixteenthOf(bar) + eighth * SIXTEENTHS_PER_CELL

    /** The sixteenth note just after the cell. */
    val end: Int get() = start + SIXTEENTHS_PER_CELL

    /** The first sixteenth note of the bar of the cell. */
    val barStart: Int get() = firstSixteenthOf(bar)

    /** The sixteenth note just after the bar of the cell. */
    val barEnd: Int get() = firstSixteenthOf(bar + 1)
}

/** Refuses with an `IllegalArgumentException` a [bar] that is not one of the music, counted from 0. */
internal fun requireBar(bar: Int) {
    require(bar in 0 until BARS) { "A bar goes from 0 to ${BARS - 1}, was $bar" }
}
