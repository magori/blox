package ch.magori.blox.core.music

/**
 * How the bass goes on after the root of the chord, which it always plays on the first beat of the bar. The
 * generator interprets each style.
 */
public enum class BassStyle {
    /** Jumping octaves, the bass of Joyful. */
    BOUNCING_OCTAVES,

    /** Whole or half notes, the bass of Calm. */
    WHOLE_AND_HALF_NOTES,

    /** Repeated eighth notes, the bass of Epic. */
    GALLOP,

    /** The root and the fifth in turn, the bass of Melancholic. */
    ROOT_AND_FIFTH,

    /** Held notes, the bass of Mysterious. */
    HELD_NOTES,

    /** Fast octaves, the bass of Retro. */
    FAST_OCTAVES,

    /** The root and the fifth in turn, marked on the beat, the bass of Dance. */
    MARKED_ROOT_AND_FIFTH,
}
