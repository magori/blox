package ch.magori.blox.core.music

/**
 * The motifs of the bass of the specification, played after the root that each chord opens on. The gallop of repeated
 * eighth notes and the held notes, half or whole notes, are one motif, the root over and over, at two note values.
 */
internal enum class BassMotif {
    /** The root and the fifth in turn. */
    ROOT_AND_FIFTH,

    /** The root, jumping an octave up and down: the bouncing bass. */
    OCTAVES,

    /** The root over and over: a gallop in eighth notes, held notes in half or whole notes. */
    REPEATED_ROOT,
}

/** How the bass of a category goes: its [motif], in notes of [beats] beats, started anew on each change of chord. */
internal data class BassStyle(val motif: BassMotif, val beats: Double) {
    init {
        requirePositiveBeats(beats, "A note of a bass style")
    }
}
