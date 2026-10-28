package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Chord
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Progression
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType

/**
 * The mood a [Garden] is heard in, after a [category] of the generated music, without its rhythm nor its melody:
 * its [scale] on C, which gives the note of each row, the [progression] of its 4 chords, one of the category on that
 * scale, one chord per group of [EIGHTHS_PER_CHORD] eighths, and its [tempo] in beats per minute, within the range of
 * the category. The tonic sits on the bottom row, on C4, so that a seven-note scale spans two octaves; on C3 for the
 * calm mood, whose pentatonic scale would otherwise climb to G6. The tempos are a first setting, to be tuned by ear.
 */
public enum class GardenMood(
    public val category: Category,
    public val scale: Scale,
    public val progression: Progression,
    public val tempo: Int,
) {
    JOYFUL(Category.JOYFUL, Scale(TONIC, ScaleType.MAJOR), Progression.parse("I–V–vi–IV"), JOYFUL_TEMPO),
    CALM(Category.CALM, Scale(CALM_TONIC, ScaleType.MAJOR_PENTATONIC), Progression.parse("I–vi–IV–V"), CALM_TEMPO),
    MYSTERIOUS(
        Category.MYSTERIOUS,
        Scale(TONIC, ScaleType.HARMONIC_MINOR),
        Progression.parse("i–♭VII–VI–V"),
        MYSTERIOUS_TEMPO,
    ),
    EPIC(Category.EPIC, Scale(TONIC, ScaleType.NATURAL_MINOR), Progression.parse("i–VI–III–VII"), EPIC_TEMPO),
    ;

    /**
     * The pitch of [row]: the tonic on the bottom row, a degree of the scale higher on each row above. A row off the
     * grid is refused with an `IllegalArgumentException`.
     */
    public fun pitchAt(row: Int): Pitch {
        require(row in 0 until Garden.ROWS) { "A garden has rows 0 to ${Garden.ROWS - 1}, was $row" }
        return scale.degree(Garden.ROWS - row)
    }

    /**
     * The chord of [column]: the chords of the progression in turn, one per group of [EIGHTHS_PER_CHORD] eighths. A
     * column off the grid is refused with an `IllegalArgumentException`.
     */
    public fun chordAt(column: Int): Chord {
        require(column in 0 until Garden.COLUMNS) { "A garden has columns 0 to ${Garden.COLUMNS - 1}, was $column" }
        return progression.chordAt(column / EIGHTHS_PER_CHORD, scale)
    }

    /** Whether the note of [cell] belongs, in any octave, to the chord of its group. */
    public fun isChordNote(cell: Position): Boolean = pitchAt(cell.row) in chordAt(cell.column)

    public companion object {
        /** The chords heard over the [Garden.COLUMNS] eighths of the grid. */
        public const val CHORDS: Int = 4

        /** The eighths of a group, under one chord: half a bar. */
        public const val EIGHTHS_PER_CHORD: Int = Garden.COLUMNS / CHORDS
    }
}

/** The tonic on the bottom row: C4, so that a seven-note scale spans two octaves, C4 to B5. */
private val TONIC = Pitch.parse("C4")

/** The tonic of the calm mood, an octave lower: its pentatonic scale spans C3 to G5, not C4 to G6. */
private val CALM_TONIC = Pitch.parse("C3")

// Tempos in beats per minute, each within the range of the category: a first setting, to be tuned by ear.
private const val JOYFUL_TEMPO = 132
private const val CALM_TEMPO = 84
private const val MYSTERIOUS_TEMPO = 100
private const val EPIC_TEMPO = 120
