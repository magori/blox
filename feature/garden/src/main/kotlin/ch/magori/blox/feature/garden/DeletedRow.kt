package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion

/**
 * How a row leaving the list stands: how much of it shows, [alpha], and how much of its height it keeps, [height], from
 * 0 to 1 each.
 */
@Immutable
internal data class RowLeaving(val alpha: Float, val height: Float)

/** How many ticks the row of a garden deleted takes to leave the list, in [effects]: none in reduced effects. */
internal fun deletedRowTicks(effects: BloxEffects): Int = if (effects.reduced) 0 else ROW_LEAVING_TICKS

/**
 * The row of a garden deleted, [tick]s after its deletion (full effects): it fades out at its height in the first half
 * of [ROW_LEAVING_TICKS], then folds up, out of sight, in the second, the rows under it coming up.
 */
internal fun deletedRow(tick: Int): RowLeaving {
    val half = ROW_LEAVING_TICKS / 2
    val fading = (tick.toFloat() / half).coerceIn(0f, 1f)
    val folding = ((tick - half).toFloat() / half).coerceIn(0f, 1f)
    return RowLeaving(alpha = 1f - fading, height = 1f - folding)
}

/** A row deleted fades then folds up in 200 ms (`designer-jeu`): `motion-row`. */
private val ROW_LEAVING_TICKS = BloxMotion.row
