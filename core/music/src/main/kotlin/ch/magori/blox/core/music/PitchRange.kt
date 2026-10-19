package ch.magori.blox.core.music

/** The pitches from [lowest] to [highest], both included: the range of a voice. */
internal data class PitchRange(val lowest: Pitch, val highest: Pitch) {
    operator fun contains(pitch: Pitch): Boolean = pitch.midi in lowest.midi..highest.midi
}
