package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType

/**
 * The saved form of a [ScoreDraft], kept in the saved state of the editor so that the draft survives the death of the
 * process: a short text, a few kilobytes for a full grid, far below what a saved state may hold.
 *
 * `1;<tonic>;<scale type>;<category or ->;<tempo>;<melody>;<bass>`, a track written `<lines>/<notes>`: the MIDI
 * numbers of its lines, then each note as `<MIDI number>.<start>.<length>` in sixteenth notes, separated by commas.
 * The lines are written whole: a line out of the scale whose notes were erased stays in the grid. The enums are written
 * by name, never by position. A form of another version, or that breaks a rule of the draft, gives no draft.
 */
internal object DraftForm {
    private const val VERSION = "1"
    private const val FIELDS = ";"
    private const val TRACK = "/"
    private const val ITEMS = ","
    private const val NOTE = "."
    private const val NO_CATEGORY = "-"
    private const val NOTE_PARTS = 3

    /** The saved form of [draft]. */
    fun write(draft: ScoreDraft): String = listOf(
        VERSION,
        draft.scale.tonic.midi.toString(),
        draft.scale.type.name,
        draft.category?.name ?: NO_CATEGORY,
        draft.tempo.bpm.toString(),
        write(draft.melody),
        write(draft.bass),
    ).joinToString(FIELDS)

    /** The draft written as [form]; `null` when it cannot be read. */
    fun read(form: String): ScoreDraft? = try {
        parse(form)
    } catch (_: IllegalArgumentException) {
        // A malformed number is a NumberFormatException, an unknown enum or a broken rule an IllegalArgumentException.
        null
    } catch (_: IndexOutOfBoundsException) {
        null
    }

    private fun write(grid: TrackGrid): String = grid.rows.joinToString(ITEMS) { it.pitch.midi.toString() } +
        TRACK +
        grid.notes.joinToString(ITEMS) { listOf(it.pitch.midi, it.start, it.length).joinToString(NOTE) }

    private fun parse(form: String): ScoreDraft? {
        val fields = form.split(FIELDS)
        if (fields.size != 7 || fields[0] != VERSION) return null
        val scale = Scale(Pitch(fields[1].toInt()), ScaleType.valueOf(fields[2]))
        return ScoreDraft(
            scale = scale,
            category = fields[3].takeUnless { it == NO_CATEGORY }?.let(Category::valueOf),
            tempo = Tempo(fields[4].toInt()),
            melody = parseGrid(fields[5], scale),
            bass = parseGrid(fields[6], scale),
        )
    }

    private fun parseGrid(
        text: String,
        scale: Scale,
    ): TrackGrid {
        val (lines, notes) = text.split(TRACK).also { require(it.size == 2) { "A track is its lines, then its notes" } }
        val rows = items(lines).map { Pitch(it.toInt()) }.map { GridRow(it, inScale = it in scale) }
        val grid = TrackGrid(
            rows,
            items(notes).map { note ->
                val parts = note.split(NOTE).map(String::toInt)
                require(parts.size == NOTE_PARTS) { "A note is its pitch, its start and its length: $note" }
                val (pitch, start, length) = parts
                GridNote(Pitch(pitch), start, length)
            },
        )
        require(grid.notes.all { note -> rows.any { it.pitch == note.pitch } }) { "Every note lies on a line" }
        return grid
    }

    private fun items(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split(ITEMS)
}
