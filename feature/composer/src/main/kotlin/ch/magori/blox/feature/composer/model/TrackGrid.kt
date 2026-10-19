package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import kotlin.math.roundToInt

/** A line of the grid, on which notes of [pitch] are placed; [inScale] is false for a note out of the scale. */
internal data class GridRow(val pitch: Pitch, val inScale: Boolean)

/**
 * The grid of one track over the whole music: its [rows], from the lowest pitch up, and its [notes], one at a time
 * (a voice is monophonic).
 */
internal data class TrackGrid(val rows: List<GridRow>, val notes: List<GridNote> = emptyList()) {
    init {
        require(notes.all { it.start >= 0 && it.end <= SIXTEENTHS }) { "Every note lies within the music: $notes" }
        require(notes.sortedBy { it.start }.zipWithNext().all { (a, b) -> a.end <= b.start }) {
            "A track plays one note at a time: $notes"
        }
    }

    /**
     * The notes of the voice that plays this grid: its notes in order, and each of its silences as one rest per bar it
     * spans. The grid keeps no rests of its own, so this writing is a normalisation: two rests side by side in a bar
     * come back as one, a rest across a bar line comes back cut there, and the music heard is the same.
     */
    fun toNotes(): List<Note> = buildList {
        var cursor = 0
        for (note in notes.sortedBy { it.start }) {
            addAll(rests(cursor, note.start))
            add(Note(note.pitch, note.length * BEATS_PER_SIXTEENTH))
            cursor = note.end
        }
        addAll(rests(cursor, SIXTEENTHS))
    }

    /**
     * Touching [cell] on the line of [pitch] erases the notes of that pitch that sound in it, from their attack to their
     * end, or else places an eighth note there, over whatever the cell held: a note that sounded there ends where the
     * new one starts.
     */
    fun place(
        pitch: Pitch,
        cell: Cell,
    ): TrackGrid {
        val touched = notesOf(pitch).filter { it.soundsIn(cell) }
        if (touched.isNotEmpty()) return copy(notes = notes - touched.toSet())
        return withNote(pitch, cell.start, cell.end)
    }

    /**
     * Touching [from] on the line of [pitch] and sliding to [to] makes a note last until the end of [to], in the bar of
     * [from]: the note of that pitch already in [from], from its attack, or else a note placed in [from].
     */
    fun slide(
        pitch: Pitch,
        from: Cell,
        to: Cell,
    ): TrackGrid {
        val start = notesOf(pitch).firstOrNull { it.soundsIn(from) }?.start ?: from.start
        return withNote(pitch, start, to.end.coerceIn(from.end, from.barEnd))
    }

    /**
     * In the Lengthen mode, touching [cell] on the line of [pitch] lengthens up to the end of [cell] the closest note of
     * that pitch on its left, in the same bar: it shortens the note when [cell] is held, brings it back to one cell when
     * [cell] is its attack, and places an eighth note when no note of that pitch starts on its left in the bar.
     */
    fun lengthen(
        pitch: Pitch,
        cell: Cell,
    ): TrackGrid = withNote(pitch, noteToLengthen(pitch, cell)?.start ?: cell.start, cell.end)

    /**
     * The note that touching [cell] on the line of [pitch] in the Lengthen mode lengthens, shortens or brings back to
     * one cell: the one of that pitch that starts closest on the left of the end of [cell] in its bar; `null` when
     * none does, and an eighth note is placed.
     */
    fun noteToLengthen(
        pitch: Pitch,
        cell: Cell,
    ): GridNote? = notesOf(pitch).filter { it.start in cell.barStart until cell.end }.maxByOrNull { it.start }

    /**
     * The grid with [bar], counted from 0, left silent, and only that bar: a note that comes into it from the bar before
     * ends at its start, a note held past its end keeps its tail in the next bar.
     */
    fun clearBar(bar: Int): TrackGrid {
        val from = firstSixteenthOf(bar)
        val until = firstSixteenthOf(bar + 1)
        return copy(
            notes = notes.flatMap { note ->
                if (note.end <= from || note.start >= until) {
                    listOf(note)
                } else {
                    listOfNotNull(
                        if (note.start < from) note.copy(length = from - note.start) else null,
                        if (note.end > until) note.copy(start = until, length = note.end - until) else null,
                    )
                }
            },
        )
    }

    /**
     * The grid with the notes that start in [bar], counted from 0, copied over the next bar: the notes that came into
     * it are cut as by [clearBar], and a copied note held past the end of [bar] stops at the bar line.
     */
    fun copyBar(bar: Int): TrackGrid {
        val start = firstSixteenthOf(bar)
        val next = firstSixteenthOf(bar + 1)
        val copies = notes.filter { it.start in start until next }
            .map { it.copy(start = it.start + SIXTEENTHS_PER_BAR, length = minOf(it.end, next) - it.start) }
        return copy(notes = clearBar(bar + 1).notes + copies)
    }

    /** Whether no note sounds in [bar], counted from 0, not even one held from the bar before. */
    fun isBarEmpty(bar: Int): Boolean =
        notes.none { it.start < firstSixteenthOf(bar + 1) && it.end > firstSixteenthOf(bar) }

    private fun notesOf(pitch: Pitch): List<GridNote> = notes.filter { it.pitch == pitch }

    /**
     * The grid with a note of [pitch] from the sixteenth note [start] until [end], over whatever was there. A pitch that
     * has no line in the grid is refused with an `IllegalArgumentException`.
     */
    private fun withNote(
        pitch: Pitch,
        start: Int,
        end: Int,
    ): TrackGrid {
        require(rows.any { it.pitch == pitch }) { "$pitch has no line in this grid" }
        return copy(notes = cleared(start, end) + GridNote(pitch, start, end - start))
    }

    /**
     * The notes, the sixteenth notes from [from] until [until] left silent: a note that sounds at [from] ends there,
     * a note that starts before [until] is gone.
     */
    private fun cleared(
        from: Int,
        until: Int,
    ): List<GridNote> = notes.mapNotNull { note ->
        when {
            note.end <= from || note.start >= until -> note
            note.start < from -> note.copy(length = from - note.start)
            else -> null
        }
    }

    /** Rests from the sixteenth note [from] until [until], one per bar. */
    private fun rests(
        from: Int,
        until: Int,
    ): List<Note> = buildList {
        var start = from
        while (start < until) {
            val end = minOf(firstSixteenthOf(start / SIXTEENTHS_PER_BAR + 1), until)
            add(Note(null, (end - start) * BEATS_PER_SIXTEENTH))
            start = end
        }
    }

    companion object {
        /** An empty grid for [track], whose rows are the notes of [scale] in the range of the track. */
        fun empty(
            track: Track,
            scale: Scale,
        ): TrackGrid = playing(track, scale, emptyList())

        /**
         * The grid of [track] that plays [voice], whose notes are written in order from the start of the music, each a
         * whole number of sixteenth notes. Its rows are the notes of [scale] in the range of the track, plus a row for
         * each note of [voice] out of them: out of the scale (a chromatic passing note), or out of the range.
         */
        fun playing(
            track: Track,
            scale: Scale,
            voice: List<Note>,
        ): TrackGrid {
            val starts = voice.runningFold(0) { start, note -> start + (note.beats / BEATS_PER_SIXTEENTH).roundToInt() }
            val notes = voice.zip(starts).mapIndexedNotNull { index, (note, start) ->
                note.pitch?.let { GridNote(it, start, starts[index + 1] - start) }
            }
            val pitches = (track.range.lowest.midi..track.range.highest.midi).map(::Pitch).filter { it in scale } +
                notes.map { it.pitch }
            val rows = pitches.distinct().sortedBy { it.midi }.map { GridRow(it, inScale = it in scale) }
            return TrackGrid(rows, notes)
        }
    }
}
