package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.rules.DropTiming

/**
 * Drop timing of Au tempo (ADR 0028): the piece drops one row on every eighth of [meter], whatever the level. The
 * fall keeps time with the bar, not with the piece: a piece that appears between two eighths drops on the next one,
 * however few ticks it waited, so [dropsAfter] does not read the ticks since the last drop. While the soft drop is
 * held, the eighths do not drop the piece: it falls one row every 2 ticks, as in the Classic.
 */
public data class TempoDropTiming(private val meter: Meter) : DropTiming {
    override fun dropsAfter(
        ticksSinceDrop: Ticks,
        tickCount: Int,
        level: Level,
        softDropping: Boolean,
    ): Boolean = !softDropping && meter.startsAnEighth(tickCount)
}
