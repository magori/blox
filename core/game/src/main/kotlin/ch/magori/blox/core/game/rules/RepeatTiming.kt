package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Ticks

/**
 * Timing of an input that repeats while it is held. Ticks are counted from the tick of the press:
 * "N ticks after the press" is tick N, the press itself is tick 0.
 *
 * @property stepsOnPress whether a step is due on the tick of the press.
 * @property firstRepeat how many ticks after the press the first repeated step comes, at least 1.
 * @property interval how many ticks separate two repeated steps, at least 1.
 */
public data class RepeatTiming(
    public val stepsOnPress: Boolean,
    public val firstRepeat: Ticks,
    public val interval: Ticks,
) {
    /** Whether a step is due [tick] ticks after the press. */
    public fun stepsOn(tick: Int): Boolean = (tick == 0 && stepsOnPress) ||
        (tick >= firstRepeat.value && (tick - firstRepeat.value) % interval.value == 0)
}
