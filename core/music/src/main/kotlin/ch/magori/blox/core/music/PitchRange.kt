package ch.magori.blox.core.music

/** The pitches from [lowest] to [highest], both included: the range of a voice. */
public data class PitchRange(public val lowest: Pitch, public val highest: Pitch) {
    public operator fun contains(pitch: Pitch): Boolean = pitch.midi in lowest.midi..highest.midi
}
