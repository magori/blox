package ch.magori.blox.core.board

import androidx.compose.runtime.Stable

/**
 * How the cells of a board are drawn on one frame, besides their look (ADR 0024): [BoardCanvas] reads it only while
 * drawing, so that an effect that plays draws the board again without recomposing it, nor building its painters again.
 * Cells are named by their column and row, so that a frame allocates nothing per cell. Its answers may change from one
 * frame to the next, read from states. At rest, as by default, the board is drawn as without effects.
 */
@Stable
interface BoardEffects {
    /** How far above its place [row] is drawn, in cells: rows that come down once rows under them are cleared. */
    fun rowLift(row: Int): Float = 0f

    /** Whether [row] glows: its background in the colour of the glow, its cells drawn without their joint. */
    fun rowGlows(row: Int): Boolean = false

    /** How large the cell at [column] and [row] is drawn, around its centre: 1 as it is, 0 not at all. */
    fun cellScale(
        column: Int,
        row: Int,
    ): Float = 1f

    /**
     * How much the cell at [column] and [row] is lit, its fill covered by the colour of the glow at this opacity: 0 not
     * at all.
     */
    fun cellLight(
        column: Int,
        row: Int,
    ): Float = 0f
}
