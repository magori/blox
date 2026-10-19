package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Pitch

/**
 * The [bar] of the line of [pitch], one character per sixteenth note: `O` where a note of that pitch starts, `=` where
 * it is held, `.` elsewhere. An eighth-note cell is two characters.
 */
internal fun TrackGrid.line(
    pitch: String,
    bar: Int = 0,
): String {
    val sixteenths = SIXTEENTHS_PER_BAR
    val first = bar * sixteenths
    return (first until first + sixteenths).joinToString("") { sixteenth ->
        val note = notes.firstOrNull { sixteenth in it.start until it.end && it.pitch == Pitch.parse(pitch) }
        when {
            note == null -> "."
            note.start == sixteenth -> "O"
            else -> "="
        }
    }
}
