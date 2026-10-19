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

/** The volume of every melody of BloX, on a square wave. */
private const val MELODY_VOLUME = 0.07

/** The volume of every bass of BloX, on a triangle wave. */
private const val BASS_VOLUME = 0.16

/** A melody of [notes], on the wave and at the volume of every melody of BloX. */
public fun melodyVoice(notes: List<Note>): Voice = Voice(Waveform.SQUARE, MELODY_VOLUME, notes)

/** A bass of [notes], on the wave and at the volume of every bass of BloX. */
public fun bassVoice(notes: List<Note>): Voice = Voice(Waveform.TRIANGLE, BASS_VOLUME, notes)

/** The range of every melody of BloX: the two octaves from C4 to C6, where the generator writes it. */
public val MELODY_RANGE: PitchRange = PitchRange(Pitch.parse("C4"), Pitch.parse("C6"))

/** The range of every bass of BloX: the two octaves from C2 to C4, where the generator writes it. */
public val BASS_RANGE: PitchRange = PitchRange(Pitch.parse("C2"), Pitch.parse("C4"))
