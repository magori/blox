package ch.magori.blox.core.music

/**
 * How often the chord changes: on the first beat of each bar, or on its strong beats 1 and 3. A bar plays
 * [chordsPerBar] chords of the progression in turn, each one for an equal share of the bar.
 */
public enum class HarmonicRhythm(internal val chordsPerBar: Int) {
    ONE_CHORD_PER_BAR(1),
    TWO_CHORDS_PER_BAR(2),
}
