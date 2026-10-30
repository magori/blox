package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.component.sheetExitTicks
import ch.magori.blox.core.designsystem.frame.rememberEffectTick
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.fadeOut

/*
 * The feedback of a garden kept from « Mes jardins » (ADR 0024), in ticks from the keeping, read in the drawing alone:
 * the sheet goes down, then the dot of the disc goes out, then the message « « X » gardé. » comes. The sound, an
 * arpeggio of the chord of the tonic, is the view model's.
 */

/**
 * How much of the dot of « Mes jardins » shows [tick]s after it went out, in [effects]: [afterSheet] a garden kept, whole
 * while the sheet goes down, then fading out within `motion-reduced-fade`, a fade even in reduced effects; otherwise
 * (Annuler, the linked garden deleted, a garden opened), fading out at once.
 */
internal fun keptDotLight(
    tick: Int,
    effects: BloxEffects,
    afterSheet: Boolean,
): Float {
    val start = if (afterSheet) sheetExitTicks(effects) else 0
    return if (tick < start) 1f else fadeOut(tick - start, BloxMotion.reducedFade)
}

/** How many ticks the dot of « Mes jardins » takes to go out once the garden is kept, in [effects]. */
internal fun keptDotTicks(effects: BloxEffects): Int = sheetExitTicks(effects) + BloxMotion.reducedFade

/**
 * Where the message of a garden kept stands [tick]s after the keeping, in [effects]: out of sight until the dot is out,
 * then rising into place as it shows, slowing down as it arrives, in `motion-card`; in reduced effects, shown in its
 * place at once.
 */
internal fun keptMessageEntrance(
    tick: Int,
    effects: BloxEffects,
): MessageEntrance {
    val shown = tick - keptDotTicks(effects)
    return when {
        shown < 0 -> MessageEntrance.Hidden

        effects.reduced -> MessageEntrance.Settled

        else -> {
            val left = 1f - (shown.toFloat() / BloxMotion.card).coerceAtMost(1f)
            val arrived = 1f - left * left
            MessageEntrance(rise = 1f - arrived, alpha = arrived)
        }
    }
}

/**
 * Where a message stands as it comes in: [rise], the part of its way up still to go (1, all of it; 0, in place), and
 * [alpha], how much of it shows.
 */
@Immutable
internal data class MessageEntrance(val rise: Float, val alpha: Float) {
    companion object {
        /** In place, whole. */
        val Settled = MessageEntrance(rise = 0f, alpha = 1f)

        /** Not come yet: out of sight, below its place. */
        val Hidden = MessageEntrance(rise = 1f, alpha = 0f)
    }
}

/**
 * The ticks since the garden of the keeping numbered [keeps] was kept (null before the garden is read), its message
 * coming in, counted on the frames of the screen and read where it is drawn; over for the keeping shown first, as on
 * coming back to the screen.
 */
@Composable
internal fun rememberKeptMessageTick(keeps: Int?): () -> Int {
    val effects = BloxTheme.effects
    return rememberEffectTick(
        key = keeps,
        move = keeps,
        frozen = null,
        length = keptDotTicks(effects) + BloxMotion.card,
    )
}
