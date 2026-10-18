package ch.magori.blox.core.music

/** A monophonic line of [notes] played with one [waveform] at a constant [volume] from 0 to 1. */
public data class Voice(public val waveform: Waveform, public val volume: Double, public val notes: List<Note>) {
    init {
        require(volume in 0.0..1.0) { "The volume is between 0 and 1, was $volume" }
        require(notes.isNotEmpty()) { "A voice has at least one note" }
    }

    /** Total duration of the voice, in beats. */
    public val beats: Double get() = notes.sumOf { it.beats }
}
