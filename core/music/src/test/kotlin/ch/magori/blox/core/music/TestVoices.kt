package ch.magori.blox.core.music

/** A well-formed one-note voice of one 4-4 bar, for tests where only the voice's validity or length matters. */
internal fun aVoice(
    volume: Double = 0.07,
    beats: Double = 4.0,
): Voice = Voice(Waveform.SQUARE, volume, listOf(Note(Pitch.parse("E5"), beats)))
