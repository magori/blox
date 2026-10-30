package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.music.BASS_RANGE
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.bassVoice
import ch.magori.blox.core.music.melodyVoice

/**
 * The identity of the score of a garden, [Garden.toScore], the same for every garden, by which the music played is
 * known. Like `LiveMusic.CALM_ID`, it names no music of the library: the score of a garden is never kept as a music
 * (no export to the saved musics in v1).
 */
public val GARDEN_SCORE_ID: ScoreId.Personal = ScoreId.Personal("garden")

/**
 * The score of this garden in its [Garden.mood], 2 bars of 4/4 at the tempo of the mood, played in a loop. Each cell
 * plays the note of its row during the eighth of its column; the cells of a piece in a column share that eighth in
 * equal parts, as a rising arpeggio, and two cells side by side on a row play two eighths, not tied. A row is a voice,
 * from the top one down, at a fixed volume, since a [Voice] plays one note at a time: two pieces in a column sound
 * together. The bass follows, on the root of the chord of each beat; an empty garden plays it alone.
 */
public fun Garden.toScore(): Score {
    val melody = struckCells()
        .groupBy(Struck::row)
        .toSortedMap()
        .map { (row, struck) -> melodyVoice(struck.toNotes(mood.pitchAt(row))).copy(volume = MELODY_VOLUME) }
    return Score(GARDEN_SCORE_ID, mood.tempo, BEATS_PER_BAR, melody + bass())
}

/** A cell of [row] heard from beat [start] to beat [end]. */
private class Struck(val row: Int, val start: Double, val end: Double)

/**
 * The cells of the pieces, each heard during its part of the eighth of its column: the n cells of a piece in a column
 * share it in n equal parts, from the lowest note up.
 */
private fun Garden.struckCells(): List<Struck> = pieces.flatMap { piece ->
    piece.cells.groupBy(Position::column).flatMap { (column, cells) ->
        val lowestFirst = cells.sortedByDescending(Position::row)
        val partStart = { part: Int -> column * EIGHTH + EIGHTH * part / lowestFirst.size }
        lowestFirst.mapIndexed { part, cell -> Struck(cell.row, partStart(part), partStart(part + 1)) }
    }
}

/**
 * The notes of a voice of [pitch] heard during these cells, with rests between them. Each note or rest lasts from
 * where the voice stands to where it ends, and not its own length: the rounding of a part that does not fall right in
 * a `Double`, a third of an eighth, never adds up. The last part of a shared eighth takes what is left of it, and the
 * voice lasts exactly 2 bars, as [Score] requires.
 */
private fun List<Struck>.toNotes(pitch: Pitch): List<Note> = sortedBy(Struck::start)
    .flatMap { listOf(Stretch(it.start, null), Stretch(it.end, pitch)) }
    .plus(Stretch(LENGTH, null))
    .fold(Voicing(), Voicing::until)
    .notes

/** A stretch of a voice, up to beat [end]: a note of [pitch], or a rest if it is `null`. */
private class Stretch(val end: Double, val pitch: Pitch?)

/** The [notes] of a voice written so far, which stands at beat [beats], their sum. */
private class Voicing(val notes: List<Note> = emptyList(), val beats: Double = 0.0) {
    /** These notes and [stretch], from where the voice stands to its end; nothing if the voice is already there. */
    fun until(stretch: Stretch): Voicing {
        val length = stretch.end - beats
        return if (length > 0) Voicing(notes + Note(stretch.pitch, length), beats + length) else this
    }
}

/**
 * The bass, on the wave of the generated basses: the root of the chord of each beat, a quarter note per beat, in the
 * octave from C2 to B2, two octaves under C4, for every mood. C2 is the bottom of the range of the generated basses;
 * two octaves under the tonic would put the calm mood, on C3, down to C1.
 */
private fun Garden.bass(): Voice = bassVoice(
    List(BEATS) { beat ->
        Note(mood.chordAt(beat * EIGHTHS_PER_BEAT).root.inOctaveFrom(BASS_OCTAVE), QUARTER)
    },
).copy(volume = BASS_VOLUME)

/**
 * The volume of each row. With [BASS_VOLUME], fourteen notes and the bass at once add up to 14 × 0.06 + 0.16 = 1.0:
 * full scale, which the mix never clips, however full the garden (decision of the developer, 2026-10-28). A first
 * setting, to be tuned by ear.
 */
private const val MELODY_VOLUME = 0.06

/** The volume of the bass: that of a generated bass. */
private const val BASS_VOLUME = 0.16

private const val EIGHTH = 0.5
private const val QUARTER = 1.0
private const val EIGHTHS_PER_BEAT = 2
private const val BEATS_PER_BAR = 4
private const val BEATS = Garden.COLUMNS / EIGHTHS_PER_BEAT
private const val LENGTH = Garden.COLUMNS * EIGHTH

/** The bottom of the octave of the bass: C2, the lowest note of the generated basses. */
private val BASS_OCTAVE = BASS_RANGE.lowest
