package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion

/**
 * Where a sheet stands as it comes in: [rise], the part of its way up still to go (1, all of it, below; 0, in place),
 * [panelAlpha], how much its panel shows, and [scrimAlpha], how much its scrim does.
 */
internal data class SheetEntrance(val rise: Float, val panelAlpha: Float, val scrimAlpha: Float) {
    companion object {
        /** In place, whole, over its whole scrim: the sheet once it has come in. */
        val Settled: SheetEntrance = SheetEntrance(rise = 0f, panelAlpha = 1f, scrimAlpha = 1f)
    }
}

/**
 * The entrance of a sheet [ticks] after it opened: it rises and shows in `motion-card`, slowing down as it arrives; in
 * reduced [effects] it does not move, it fades in within `motion-reduced-fade`. Its scrim, a fade, fades in within
 * `motion-reduced-fade` either way.
 */
internal fun sheetEntrance(
    ticks: Int,
    effects: BloxEffects,
): SheetEntrance {
    val panel = easeOut(ticks, if (effects.reduced) BloxMotion.reducedFade else BloxMotion.card)
    return SheetEntrance(
        rise = if (effects.reduced) 0f else 1f - panel,
        panelAlpha = panel,
        scrimAlpha = easeOut(ticks, BloxMotion.reducedFade),
    )
}

/**
 * How many ticks a sheet closed takes to go down, in [effects]: `motion-card`; none in reduced effects, where it is
 * gone at once.
 */
fun sheetExitTicks(effects: BloxEffects): Int = if (effects.reduced) 0 else BloxMotion.card

/**
 * Where a sheet closed stands [ticks] after it started to go down (full effects): from in place, it goes down and
 * fades out with its scrim in `motion-card`, slowly at first, faster and faster as it goes.
 */
internal fun sheetExit(
    ticks: Int,
    effects: BloxEffects,
): SheetEntrance {
    val length = sheetExitTicks(effects)
    val progress = if (length == 0) 1f else (ticks.toFloat() / length).coerceIn(0f, 1f)
    val gone = progress * progress
    return SheetEntrance(rise = gone, panelAlpha = 1f - gone, scrimAlpha = 1f - gone)
}

/** Fast at first, slowing down as it ends: from 0 to 1 in [duration] ticks, at [ticks]. */
private fun easeOut(
    ticks: Int,
    duration: Int,
): Float {
    val left = 1f - (ticks.toFloat() / duration).coerceAtMost(1f)
    return 1f - left * left
}

/**
 * The shape of a sheet, from the [card] of its theme (`radius-card`): its corners at the top; at the bottom, on the edge
 * of the screen, square ones. A square card stays square.
 */
internal fun sheetShape(card: Shape): Shape = if (card is CornerBasedShape) {
    card.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
} else {
    card
}
