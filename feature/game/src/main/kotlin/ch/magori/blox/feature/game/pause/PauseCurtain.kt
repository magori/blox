package ch.magori.blox.feature.game.pause

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion

/** Where the curtain of the pause stands. */
internal enum class CurtainPhase {
    /** It comes down over the board: the card waits. */
    CLOSING,

    /** It hides the board: the card shows. */
    CLOSED,

    /** It goes back up: the game waits. */
    OPENING,

    /** It is gone: the game may go on. */
    OPEN,
}

/** The curtain of the pause, counted in ticks of 1/60 s. */
@Immutable
internal data class PauseCurtain(val shut: Int, val duration: Int, val closing: Boolean, val fades: Boolean) {
    val phase: CurtainPhase
        get() = when {
            closing -> if (shut == duration) CurtainPhase.CLOSED else CurtainPhase.CLOSING
            else -> if (shut == 0) CurtainPhase.OPEN else CurtainPhase.OPENING
        }

    /** How opaque the rows it covers are. */
    val opacity: Float get() = if (fades) shut.toFloat() / duration else 1f

    /** How many of [rows], from the top, the curtain covers: a row as soon as the curtain reaches into it. */
    fun coveredRows(rows: Int): Int = when {
        fades -> if (shut > 0) rows else 0
        else -> (shut * rows + duration - 1) / duration
    }

    /** The curtain [ticks] later. */
    fun advance(ticks: Int): PauseCurtain =
        copy(shut = if (closing) minOf(duration, shut + ticks) else maxOf(0, shut - ticks))

    /** The curtain coming back down, from where it stands. */
    fun comingDown(): PauseCurtain = copy(closing = true)

    /** The curtain going back up. */
    fun opening(): PauseCurtain = copy(closing = false)

    companion object {
        /** A curtain that starts to close. */
        fun closing(effects: BloxEffects): PauseCurtain = PauseCurtain(
            shut = 0,
            duration = if (effects.reduced) BloxMotion.reducedFade else BloxMotion.curtain,
            closing = true,
            fades = effects.reduced,
        )

        /** A curtain already open: Reprendre came before the curtain, the game may go on at once. */
        fun open(): PauseCurtain = PauseCurtain(shut = 0, duration = BloxMotion.curtain, closing = false, fades = false)

        /** A curtain already shut: the game showed paused, after a recreation of the activity for instance. */
        fun closed(effects: BloxEffects): PauseCurtain = closing(effects).let { it.advance(it.duration) }
    }
}
