package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TrackGridBarsTest {
    private val empty = TrackGrid.empty(Track.MELODY, Scale(Pitch.parse("C4"), ScaleType.MAJOR))

    @Test
    fun `clearing a bar silences it and leaves the other bars as they were`() {
        // Given a note in each of the first two bars
        val grid = empty.place(Pitch.parse("E4"), Cell(bar = 0, eighth = 0))
            .place(Pitch.parse("G4"), Cell(bar = 1, eighth = 3))

        // When
        val cleared = grid.clearBar(1)

        // Then
        assertThat(cleared.line("E4", bar = 0)).isEqualTo("O=..............")
        assertThat(cleared.line("G4", bar = 1)).isEqualTo("................")
    }

    @Test
    fun `a bar is empty when no note sounds in it, even held from the bar before`() {
        // Given a whole note and a half from the start of the first bar
        val grid = empty.copy(notes = listOf(GridNote(Pitch.parse("E4"), 0, 24)))

        // Then
        assertThat(grid.isBarEmpty(1)).isFalse()
        assertThat(grid.isBarEmpty(2)).isTrue()
    }

    @Test
    fun `copying a bar replaces the next bar with its notes`() {
        // Given a long note in the first bar and an eighth note in the second
        val grid = empty.slide(Pitch.parse("E4"), from = Cell(bar = 0, eighth = 1), to = Cell(bar = 0, eighth = 2))
            .place(Pitch.parse("G4"), Cell(bar = 1, eighth = 5))

        // When
        val copied = grid.copyBar(0)

        // Then
        assertThat(copied.line("E4", bar = 0)).isEqualTo("..O===..........")
        assertThat(copied.line("E4", bar = 1)).isEqualTo("..O===..........")
        assertThat(copied.line("G4", bar = 1)).isEqualTo("................")
    }

    @Test
    fun `a note held over the bar line is copied up to the bar line only`() {
        // Given a half note across the line between the first two bars
        val grid = empty.copy(notes = listOf(GridNote(Pitch.parse("E4"), 12, 8)))

        // When
        val copied = grid.copyBar(0)

        // Then
        assertThat(copied.line("E4", bar = 0)).isEqualTo("............O===")
        assertThat(copied.line("E4", bar = 1)).isEqualTo("............O===")
        assertThat(copied.line("E4", bar = 2)).isEqualTo("................")
    }

    @Test
    fun `clearing a bar leaves the next bar the tail of a note held over the bar line`() {
        // Given a half note across the line between the first two bars
        val grid = empty.copy(notes = listOf(GridNote(Pitch.parse("E4"), 12, 8)))

        // When
        val cleared = grid.clearBar(0)

        // Then
        assertThat(cleared.notes).containsExactly(GridNote(Pitch.parse("E4"), 16, 4))
    }

    @Test
    fun `clearing a bar that a long note crosses keeps the note before and after the bar`() {
        // Given a note from the end of the first bar to the start of the third
        val grid = empty.copy(notes = listOf(GridNote(Pitch.parse("E4"), 12, 24)))

        // When
        val cleared = grid.clearBar(1)

        // Then
        assertThat(
            cleared.notes,
        ).containsExactly(GridNote(Pitch.parse("E4"), 12, 4), GridNote(Pitch.parse("E4"), 32, 4))
    }
}
