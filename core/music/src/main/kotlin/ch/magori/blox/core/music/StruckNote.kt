package ch.magori.blox.core.music

/** A note of [pitch] struck in [bar], counted from 0, at [onset] beats from the start of the bar. */
internal data class StruckNote(val bar: Int, val onset: Double, val pitch: Pitch)

/** The notes struck in [bars], rests left out, in the order they are played. */
internal fun struckIn(bars: List<List<Note>>): List<StruckNote> =
    eachStruckIn(bars) { bar, onset, _, pitch -> StruckNote(bar, onset, pitch) }

/**
 * The notes struck in [bars], rests left out, in the order they are played, each turned by [transform] from its bar,
 * counted from 0, its onset in beats from the start of the bar, the note and its pitch.
 */
internal fun <T> eachStruckIn(
    bars: List<List<Note>>,
    transform: (bar: Int, onset: Double, note: Note, pitch: Pitch) -> T,
): List<T> = bars.flatMapIndexed { bar, notes ->
    val onsets = notes.runningFold(0.0) { onset, note -> onset + note.beats }
    notes.zip(onsets).mapNotNull { (note, onset) -> note.pitch?.let { transform(bar, onset, note, it) } }
}
