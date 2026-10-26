package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The tags of the screen, the same in every language, by which the journey of `:benchmark` finds what it touches. */
class LocksmithTestTagsTest {
    @Test
    fun `a cell of the padlock is tagged by what it shows, the hole apart from the rest`() {
        // When
        val tags = listOf(
            LockCell.Hole,
            LockCell.Metal,
            LockCell.Hint,
            LockCell.Placed(PieceType.T),
            LockCell.Preview(PieceType.T, fits = true),
        ).map(::cellTestTag)

        // Then
        assertThat(tags).containsExactly(
            "locksmith_cell_hole",
            "locksmith_cell_metal",
            "locksmith_cell_hint",
            "locksmith_cell_placed",
            "locksmith_cell_preview",
        )
    }

    @Test
    fun `each tile of a given piece is tagged by its rank`() {
        // When
        val tags = (0..2).map(::tileTestTag)

        // Then
        assertThat(tags).containsExactly("locksmith_tile_0", "locksmith_tile_1", "locksmith_tile_2")
    }
}
