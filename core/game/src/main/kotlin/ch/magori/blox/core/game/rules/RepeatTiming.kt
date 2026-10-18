package ch.magori.blox.core.game.rules

/**
 * Timing of an input that repeats while it is held. Ticks are counted from the tick of the press:
 * "N ticks after the press" is tick N, the press itself is tick 0.
 *
 * @property stepsOnPress whether a step is due on the tick of the press.
 * @property firstRepeat how many ticks after the press the first repeated step comes.
 * @property interval how many ticks separate two repeated steps.
 */
public data class RepeatTiming(
    public val stepsOnPress: Boolean,
    public val firstRepeat: Int,
    public val interval: Int,
) {
    init {
        require(firstRepeat > 0) { "The first repeat comes at least 1 tick after the press: $firstRepeat" }
        require(interval > 0) { "A repeat interval is at least 1 tick: $interval" }
    }

    /** Whether a step is due [tick] ticks after the press. */
    public fun stepsOn(tick: Int): Boolean =
        (tick == 0 && stepsOnPress) || (tick >= firstRepeat && (tick - firstRepeat) % interval == 0)
}
