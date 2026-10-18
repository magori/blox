package ch.magori.blox.core.music

/** A well-formed one-note voice, for tests where only the voice's validity matters. */
internal fun aVoice(volume: Double = 0.07): Voice = Voice(Waveform.SQUARE, volume, listOf(Note(Pitch.parse("E5"), 1.0)))
