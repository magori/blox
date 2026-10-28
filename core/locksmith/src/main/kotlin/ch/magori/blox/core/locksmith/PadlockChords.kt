package ch.magori.blox.core.locksmith

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Progression
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType

/**
 * The chords of a padlock, one per piece: a phrase that always ends on the perfect cadence V–I, so that
 * the last piece placed resolves it.
 */
public object PadlockChords {
    /** The key the chords are heard in: C major, its tonic on C4. */
    public val KEY: Scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR)

    private val PROGRESSIONS = mapOf(
        3 to "IV-V-I",
        4 to "I-IV-V-I",
        5 to "I-vi-IV-V-I",
        6 to "I-vi-ii-IV-V-I",
        7 to "I-V-vi-iii-IV-V-I",
        8 to "I-V-vi-iii-IV-ii-V-I",
    ).mapValues { (_, chords) -> Progression.parse(chords) }

    init {
        check(PROGRESSIONS.keys == Padlock.PIECE_COUNTS.toSet()) { "One progression per number of pieces" }
    }

    /** The chords of a padlock of [pieceCount] pieces, in the order they are heard. */
    public fun progression(pieceCount: Int): Progression = requireNotNull(PROGRESSIONS[pieceCount]) {
        "A padlock holds ${Padlock.PIECE_COUNTS.first} to ${Padlock.PIECE_COUNTS.last} pieces, got $pieceCount"
    }
}
