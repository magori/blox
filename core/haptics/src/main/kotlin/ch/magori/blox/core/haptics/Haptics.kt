package ch.magori.blox.core.haptics

/**
 * The haptic feedback of the device: one short vibration pattern per [Haptic]. [SystemHaptics] vibrates the
 * device; whether the player wants vibrations at all is decided by the caller, before it asks.
 */
public interface Haptics {
    /** Plays [haptic] at once; a device without a vibrator plays nothing. */
    public fun play(haptic: Haptic)
}

/** What a vibration tells the player, from the lightest to the strongest. */
public enum class Haptic {
    /** One to three lines were cleared at once: a short pulse, felt and forgotten. */
    LINES_CLEARED,

    /** Four lines were cleared at once, the best move: a stronger pulse. */
    FOUR_LINES_CLEARED,

    /** The game is over: a pattern of its own, longer, felt once per game. */
    GAME_OVER,
}
