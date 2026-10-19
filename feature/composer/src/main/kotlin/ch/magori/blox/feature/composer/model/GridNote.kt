package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Pitch

/** A note of [pitch] that starts at the [start] sixteenth note of the music and lasts [length] sixteenth notes. */
internal data class GridNote(val pitch: Pitch, val start: Int, val length: Int) {
    init {
        require(length > 0) { "A note lasts at least a sixteenth note, was $length" }
    }

    /** The sixteenth note just after the note. */
    val end: Int get() = start + length

    /** Whether the note sounds during some of [cell]. */
    fun soundsIn(cell: Cell): Boolean = start < cell.end && end > cell.start
}
