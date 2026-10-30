package ch.magori.blox.core.music

/** A well-formed one-note voice of one 4-4 bar, for tests where only the voice's validity or length matters. */
internal fun aVoice(
    volume: Double = 0.07,
    beats: Double = 4.0,
): Voice = Voice(Waveform.SQUARE, volume, listOf(Note(Pitch.parse("E5"), beats)))

/** A melody written bar by bar, as notes such as `"C4 1"` separated by commas, `r` marking a rest. */
internal fun melodyOf(vararg bars: String): Melody = Melody(barsOf(bars))

/** A bass written bar by bar, as notes such as `"C3 1"` separated by commas. */
internal fun bassOf(vararg bars: String): Bass = Bass(barsOf(bars))

private fun barsOf(bars: Array<out String>): List<List<Note>> = bars.map { bar ->
    bar.split(", ").map { note ->
        val (pitch, beats) = note.split(" ")
        Note(if (pitch == "r") null else Pitch.parse(pitch), beats.toDouble())
    }
}

/** Cuts [voice] into bars: a new bar starts once the current one holds [beatsPerBar] beats or more. */
internal fun barsOf(
    voice: Voice,
    beatsPerBar: Int,
): List<List<Note>> {
    val bars = mutableListOf(mutableListOf<Note>())
    voice.notes.forEach { note ->
        if (bars.last().sumOf { it.beats } >= beatsPerBar) bars += mutableListOf<Note>()
        bars.last() += note
    }
    return bars
}
