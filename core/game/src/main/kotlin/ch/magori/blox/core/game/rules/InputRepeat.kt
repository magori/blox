package ch.magori.blox.core.game.rules

/**
 * Held inputs policy: how a held input repeats. The policy is interchangeable: [ClassicInputRepeat]
 * holds the NES values.
 */
public interface InputRepeat {
    /** Timing of a held left or right shift (DAS). */
    public val shift: RepeatTiming

    /** Timing of a held soft drop. */
    public val softDrop: RepeatTiming
}
