package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class TrackGridTest {
    private val cMajor = Scale(Pitch.parse("C4"), ScaleType.MAJOR)

    @Test
    fun `the melody rows are the notes of the scale from C4 to C6`() {
        // Given / When
        val grid = TrackGrid.empty(Track.MELODY, cMajor)

        // Then
        assertThat(grid.rows.map { it.pitch.toString() }).containsExactly(
            "C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6",
        )
    }

    @Test
    fun `the bass rows are the notes of the scale from C2 to C4, a pentatonic scale leaving out its missing degrees`() {
        // Given
        val gPentatonic = Scale(Pitch.parse("G4"), ScaleType.MAJOR_PENTATONIC)

        // When
        val grid = TrackGrid.empty(Track.BASS, gPentatonic)

        // Then
        assertThat(grid.rows.map { it.pitch.toString() }).containsExactly(
            "D2", "E2", "G2", "A2", "B2", "D3", "E3", "G3", "A3", "B3",
        )
    }

    @Test
    fun `a note is placed only on a line of the grid`() {
        // Given
        val grid = TrackGrid.empty(Track.MELODY, cMajor)

        // Then F#4 is out of C major, and C3 below the melody
        assertThatIllegalArgumentException().isThrownBy { grid.place(Pitch.parse("F#4"), Cell(bar = 0, eighth = 0)) }
        assertThatIllegalArgumentException().isThrownBy { grid.place(Pitch.parse("C3"), Cell(bar = 0, eighth = 0)) }
    }

    @Test
    fun `touching an empty cell places an eighth note`() {
        // Given
        val grid = TrackGrid.empty(Track.MELODY, cMajor)

        // When
        val placed = grid.place(Pitch.parse("E4"), Cell(bar = 0, eighth = 2))

        // Then
        assertThat(placed.line("E4")).isEqualTo("....O=..........")
        assertThat(placed.notes).hasSize(1)
    }

    @Test
    fun `touching a note erases it`() {
        // Given
        val grid = TrackGrid.empty(Track.MELODY, cMajor).place(Pitch.parse("E4"), Cell(bar = 0, eighth = 2))

        // When
        val erased = grid.place(Pitch.parse("E4"), Cell(bar = 0, eighth = 2))

        // Then
        assertThat(erased.notes).isEmpty()
    }

    @Test
    fun `placing a note in the middle of a long note cuts the long note there`() {
        // Given a half note on C4
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("C4"), 0, 8)))

        // When
        val placed = grid.place(Pitch.parse("E4"), Cell(bar = 0, eighth = 2))

        // Then
        assertThat(placed.line("C4")).isEqualTo("O===............")
        assertThat(placed.line("E4")).isEqualTo("....O=..........")
        assertThat(placed.notes).hasSize(2)
    }

    @Test
    fun `placing a note where another pitch starts replaces that note, even the part held after the cell`() {
        // Given a quarter note on D4 from the third cell
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("D4"), 4, 4)))

        // When
        val placed = grid.place(Pitch.parse("E4"), Cell(bar = 0, eighth = 2))

        // Then
        assertThat(placed.line("D4")).isEqualTo("................")
        assertThat(placed.line("E4")).isEqualTo("....O=..........")
        assertThat(placed.notes).hasSize(1)
    }

    @Test
    fun `sliding to the right from an empty cell places a note that lasts until the cell where the finger is lifted`() {
        // Given
        val grid = TrackGrid.empty(Track.MELODY, cMajor)

        // When
        val slid = grid.slide(Pitch.parse("E4"), from = Cell(bar = 0, eighth = 1), to = Cell(bar = 0, eighth = 3))

        // Then
        assertThat(slid.line("E4")).isEqualTo("..O=====........")
        assertThat(slid.notes).hasSize(1)
    }

    @Test
    fun `sliding from a note of the same line lengthens that note from its attack`() {
        // Given a quarter note on E4
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("E4"), 0, 4)))

        // When the finger goes down on its held cell
        val slid = grid.slide(Pitch.parse("E4"), from = Cell(bar = 0, eighth = 1), to = Cell(bar = 0, eighth = 3))

        // Then
        assertThat(slid.line("E4")).isEqualTo("O=======........")
        assertThat(slid.notes).hasSize(1)
    }

    @Test
    fun `a note slid past the end of its bar stops at the bar line`() {
        // Given
        val grid = TrackGrid.empty(Track.MELODY, cMajor)

        // When
        val slid = grid.slide(Pitch.parse("E4"), from = Cell(bar = 0, eighth = 6), to = Cell(bar = 1, eighth = 2))

        // Then
        assertThat(slid.line("E4", bar = 0)).isEqualTo("............O===")
        assertThat(slid.line("E4", bar = 1)).isEqualTo("................")
        assertThat(slid.notes).hasSize(1)
    }

    @Test
    fun `sliding to the left places an eighth note`() {
        // Given
        val grid = TrackGrid.empty(Track.MELODY, cMajor)

        // When
        val slid = grid.slide(Pitch.parse("E4"), from = Cell(bar = 0, eighth = 3), to = Cell(bar = 0, eighth = 1))

        // Then
        assertThat(slid.line("E4")).isEqualTo("......O=........")
        assertThat(slid.notes).hasSize(1)
    }

    @Test
    fun `a slid note covers the notes it passes over`() {
        // Given an eighth note on G4 in the third cell
        val grid = TrackGrid.empty(Track.MELODY, cMajor).place(Pitch.parse("G4"), Cell(bar = 0, eighth = 2))

        // When
        val slid = grid.slide(Pitch.parse("E4"), from = Cell(bar = 0, eighth = 0), to = Cell(bar = 0, eighth = 3))

        // Then
        assertThat(slid.line("G4")).isEqualTo("................")
        assertThat(slid.line("E4")).isEqualTo("O=======........")
        assertThat(slid.notes).hasSize(1)
    }

    @Test
    fun `in Lengthen mode, touching a cell to the right of a note lengthens the note up to there`() {
        // Given an eighth note on E4 in the second cell
        val grid = TrackGrid.empty(Track.MELODY, cMajor).place(Pitch.parse("E4"), Cell(bar = 0, eighth = 1))

        // When
        val lengthened = grid.lengthen(Pitch.parse("E4"), Cell(bar = 0, eighth = 4))

        // Then
        assertThat(lengthened.line("E4")).isEqualTo("..O=======......")
        assertThat(lengthened.notes).hasSize(1)
    }

    @Test
    fun `in Lengthen mode, touching a held cell shortens the note to end there`() {
        // Given a half note on E4
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("E4"), 0, 8)))

        // When
        val shortened = grid.lengthen(Pitch.parse("E4"), Cell(bar = 0, eighth = 1))

        // Then
        assertThat(shortened.line("E4")).isEqualTo("O===............")
        assertThat(shortened.notes).hasSize(1)
    }

    @Test
    fun `in Lengthen mode, touching the attack of a note brings it back to one cell`() {
        // Given a half note on E4
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("E4"), 0, 8)))

        // When
        val shortened = grid.lengthen(Pitch.parse("E4"), Cell(bar = 0, eighth = 0))

        // Then
        assertThat(shortened.line("E4")).isEqualTo("O=..............")
        assertThat(shortened.notes).hasSize(1)
    }

    @Test
    fun `in Lengthen mode, touching a cell with no note of its line on its left in the bar places an eighth note`() {
        // Given an eighth note on E4 at the end of the first bar
        val grid = TrackGrid.empty(Track.MELODY, cMajor).place(Pitch.parse("E4"), Cell(bar = 0, eighth = 7))

        // When
        val placed = grid.lengthen(Pitch.parse("E4"), Cell(bar = 1, eighth = 2))

        // Then
        assertThat(placed.line("E4", bar = 0)).isEqualTo("..............O=")
        assertThat(placed.line("E4", bar = 1)).isEqualTo("....O=..........")
        assertThat(placed.notes).hasSize(2)
    }

    @Test
    fun `touching the held part of a note erases the whole note`() {
        // Given a half note on E4
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("E4"), 0, 8)))

        // When
        val erased = grid.place(Pitch.parse("E4"), Cell(bar = 0, eighth = 2))

        // Then
        assertThat(erased.notes).isEmpty()
    }

    @Test
    fun `touching one of two notes that share a cell erases that note only, and a held note whole`() {
        // Given a dotted eighth on G4, then a sixteenth on D5 in the second half of the second cell
        val grid = TrackGrid.empty(Track.MELODY, cMajor)
            .copy(notes = listOf(GridNote(Pitch.parse("G4"), 0, 3), GridNote(Pitch.parse("D5"), 3, 1)))

        // When
        val withoutD5 = grid.place(Pitch.parse("D5"), Cell(bar = 0, eighth = 1))
        val withoutG4 = grid.place(Pitch.parse("G4"), Cell(bar = 0, eighth = 1))

        // Then
        assertThat(withoutD5.notes).containsExactly(GridNote(Pitch.parse("G4"), 0, 3))
        assertThat(withoutG4.notes).containsExactly(GridNote(Pitch.parse("D5"), 3, 1))
    }

    @Test
    fun `in Lengthen mode, a lengthened note covers the notes of other lines between it and the cell`() {
        // Given an eighth note on E4, then one on G4 two cells later
        val grid = TrackGrid.empty(Track.MELODY, cMajor)
            .place(Pitch.parse("E4"), Cell(bar = 0, eighth = 0))
            .place(Pitch.parse("G4"), Cell(bar = 0, eighth = 2))

        // When
        val lengthened = grid.lengthen(Pitch.parse("E4"), Cell(bar = 0, eighth = 3))

        // Then
        assertThat(lengthened.notes).containsExactly(GridNote(Pitch.parse("E4"), 0, 8))
    }

    @Test
    fun `in Lengthen mode, touching an attack in the middle of a cell brings the note back to the end of that cell`() {
        // Given a note on D5 from the second half of the second cell, held for five sixteenth notes
        val grid = TrackGrid.empty(Track.MELODY, cMajor).copy(notes = listOf(GridNote(Pitch.parse("D5"), 3, 5)))

        // When
        val shortened = grid.lengthen(Pitch.parse("D5"), Cell(bar = 0, eighth = 1))

        // Then a sixteenth note, its attack left where it was
        assertThat(shortened.notes).containsExactly(GridNote(Pitch.parse("D5"), 3, 1))
    }

    @Test
    fun `a grid holds its notes within the eight bars, one at a time`() {
        // Given
        val empty = TrackGrid.empty(Track.MELODY, cMajor)
        val e4 = Pitch.parse("E4")

        // Then
        assertThatIllegalArgumentException().isThrownBy { empty.copy(notes = listOf(GridNote(e4, -1, 2))) }
        assertThatIllegalArgumentException().isThrownBy { empty.copy(notes = listOf(GridNote(e4, 127, 2))) }
        assertThatIllegalArgumentException().isThrownBy {
            empty.copy(notes = listOf(GridNote(e4, 0, 4), GridNote(Pitch.parse("G4"), 3, 2)))
        }
    }
}
