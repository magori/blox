package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.model.Ticks

/**
 * The meter of Au tempo: how the ticks (1/60 s) of a game divide into eighths, quarter notes, bars of 4 beats and
 * phrases of 4 bars. Tick 0 is beat 1 of bar 1; positions are read from
 * [ch.magori.blox.core.game.engine.GameState.tickCount], never negative.
 *
 * @property ticksPerEighth how long an eighth lasts, a whole number of ticks: it sets the tempo, [beatsPerMinute].
 * @property onBeatWindow how many ticks before or after a quarter note a tick is still on the beat, see [isOnBeat]:
 * shorter than an eighth, so that the off-beat between two quarter notes is never on the beat.
 */
public data class Meter(
    public val ticksPerEighth: Ticks,
    public val onBeatWindow: Ticks = PROVISIONAL_ON_BEAT_WINDOW,
) {
    init {
        require(onBeatWindow.value < ticksPerEighth.value) {
            "A window of ${onBeatWindow.value} ticks reaches the off-beat of an eighth of ${ticksPerEighth.value} ticks"
        }
    }

    /** The tempo, in quarter notes per minute: 1 800 ticks per half minute, over the ticks of an eighth. */
    public val beatsPerMinute: Double get() = TICKS_PER_HALF_MINUTE / ticksPerEighth.value

    /** A quarter note, one beat: 2 eighths. */
    public val ticksPerQuarter: Ticks get() = eighths(EIGHTHS_PER_QUARTER)

    /** A bar: 4 beats, 8 eighths. */
    public val ticksPerBar: Ticks get() = eighths(EIGHTHS_PER_QUARTER * BEATS_PER_BAR)

    /** A phrase: 4 bars, 32 eighths. */
    public val ticksPerPhrase: Ticks get() = eighths(EIGHTHS_PER_QUARTER * BEATS_PER_BAR * BARS_PER_PHRASE)

    /** Whether an eighth starts at [tickCount]: every [ticksPerEighth] ticks from tick 0. */
    public fun startsAnEighth(tickCount: Int): Boolean = checked(tickCount) % ticksPerEighth.value == 0

    /** The beat of the bar at [tickCount], from 1 to 4. */
    public fun beatOf(tickCount: Int): Int = checked(tickCount) % ticksPerBar.value / ticksPerQuarter.value + 1

    /** The bar of the phrase at [tickCount], from 1 to 4. */
    public fun barOf(tickCount: Int): Int = checked(tickCount) % ticksPerPhrase.value / ticksPerBar.value + 1

    /** Whether [tickCount] lies within [onBeatWindow] ticks of a quarter note, any beat of the bar, edges included. */
    public fun isOnBeat(tickCount: Int): Boolean {
        val sinceQuarter = checked(tickCount) % ticksPerQuarter.value
        return minOf(sinceQuarter, ticksPerQuarter.value - sinceQuarter) <= onBeatWindow.value
    }

    private fun eighths(count: Int): Ticks = Ticks(count * ticksPerEighth.value)

    private fun checked(tickCount: Int): Int {
        require(tickCount >= 0) { "A tick count cannot be negative: $tickCount" }
        return tickCount
    }

    public companion object {
        /**
         * The window on the beat until it is measured: 5 ticks (about 83 ms) on each side of a quarter note.
         * (à vérifier) 5 or 6 ticks, to be set by the measure on a phone, audio latency included (P11.2).
         */
        public val PROVISIONAL_ON_BEAT_WINDOW: Ticks = Ticks(5)

        private const val TICKS_PER_HALF_MINUTE = 1800.0
        private const val EIGHTHS_PER_QUARTER = 2
        private const val BEATS_PER_BAR = 4
        private const val BARS_PER_PHRASE = 4
    }
}
