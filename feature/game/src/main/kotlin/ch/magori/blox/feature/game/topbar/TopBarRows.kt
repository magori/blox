package ch.magori.blox.feature.game.topbar

/** How the top bar lays out the pause button, the mode title and the best. */
internal enum class TopBarRows {
    /** On one row, the title and the best sharing the room beside the pause button in halves. */
    ONE,

    /** The title on a row of its own, as wide as the bar, above the pause button and the best. */
    TITLE_ABOVE,
}

/**
 * The rows of the top bar, given the [room] beside the pause button and its gaps: one row if the title and the best
 * each fit whole in their half of it, as the row shares it, at the smallest size they may shrink to ([titleWidth],
 * [bestWidth]), within [ROUNDING_MARGIN]; otherwise the title goes above, where it has the whole width of the bar.
 * The widths are in pixels.
 */
internal fun topBarRows(
    room: Float,
    titleWidth: Float,
    bestWidth: Float,
): TopBarRows {
    val half = room / 2 - ROUNDING_MARGIN
    return if (titleWidth <= half && bestWidth <= half) TopBarRows.ONE else TopBarRows.TITLE_ABOVE
}

/** A pixel spared in each half: the row rounds the widths it gives, and a text measured at the limit must not be cut. */
private const val ROUNDING_MARGIN = 1f
