package ch.magori.blox.feature.home

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicGravityTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PreviewReplayTest {
    @Test
    fun `the preview drops a row every ticks per row of the chosen level`() {
        // Given the preview at level 5, its T just in
        val level = Level(5)
        val ticksPerRow = ClassicGravityTable.ticksPerRow(level).value
        val start = PreviewReplay(level)
        val startRow = start.piece!!.pivot.row

        // When a tick short of a row, then that row's tick
        val waiting = start.advancedBy(ticksPerRow - 1)
        val dropped = start.advancedBy(ticksPerRow)

        // Then the T waits, then drops one row
        assertThat(waiting.piece!!.pivot.row).isEqualTo(startRow)
        assertThat(dropped.piece!!.pivot.row).isEqualTo(startRow + 1)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 5, 9])
    fun `the preview scores 100 times the level plus one for its two lines`(level: Int) {
        // Given the preview at the level, its T on its way down, no line cleared yet
        val start = PreviewReplay(Level(level))
        assertThat(start.points).isEqualTo(Points(0))

        // When the T has landed and locked
        val cleared = start.untilLocked()

        // Then two lines are gone from the board, and their points are those of the Classic mode
        assertThat(cleared.piece).isNull()
        assertThat(cleared.board.stackHeight).isEqualTo(start.board.stackHeight - 2)
        assertThat(cleared.points).isEqualTo(Points(100 * (level + 1)))
    }

    @Test
    fun `a preview given a new level starts again with a new T, at the gravity of that level`() {
        // Given the preview at level 0, its two lines cleared
        val played = PreviewReplay(Level(0)).untilLocked()

        // When the level changes to 9
        val changed = played.withLevel(Level(9))

        // Then a new T comes in, without points, and falls at the gravity of level 9
        assertThat(changed).isEqualTo(PreviewReplay(Level(9)))
        val ticksPerRow = ClassicGravityTable.ticksPerRow(Level(9)).value
        assertThat(changed.advancedBy(ticksPerRow).piece!!.pivot.row).isEqualTo(changed.piece!!.pivot.row + 1)
    }

    @Test
    fun `the preview goes on where it is when the level stays the same`() {
        // Given the preview at level 3, on its way
        val playing = PreviewReplay(Level(3)).advancedBy(40)

        // When the same level is chosen again
        val same = playing.withLevel(Level(3))

        // Then nothing starts again
        assertThat(same).isEqualTo(playing)
    }

    @Test
    fun `the preview plays again once its two lines have shown cleared for 2 seconds`() {
        // Given the preview at level 9, its T just locked
        val cleared = PreviewReplay(Level(9)).untilLocked()

        // When a tick short of 2 seconds, then 2 seconds later
        val showing = cleared.advancedBy(SHOWN_CLEARED - 1)
        val again = cleared.advancedBy(SHOWN_CLEARED)

        // Then the lines show cleared, then a new T comes in, without points, on the stack with its holes
        assertThat(showing.piece).isNull()
        assertThat(again).isEqualTo(PreviewReplay(Level(9)))
    }

    @Test
    fun `the still preview, for reduced effects, shows its two lines cleared and their points`() {
        // Given the level 4
        val level = Level(4)

        // When the preview stands still
        val still = PreviewReplay.still(level)

        // Then it shows what the replay ends on
        val ended = PreviewReplay(level).untilLocked()
        assertThat(still.board).isEqualTo(ended.board)
        assertThat(still.piece).isNull()
        assertThat(still.points).isEqualTo(ended.points)
    }

    @Test
    fun `the preview scores 000000, then 000600 once its lines clear at level 5, and 000600 at once still`() {
        // Given the preview at level 5
        val start = PreviewReplay(Level(5))

        // Then the score it shows, as in a game, on 6 digits; the two it may show, laid out before
        assertThat(previewScore(start.frame)).isEqualTo("000000")
        assertThat(previewScore(start.untilLocked().frame)).isEqualTo("000600")
        assertThat(previewScore(PreviewReplay.still(Level(5)).frame)).isEqualTo("000600")
        assertThat(previewScores(Level(5))).containsExactly("000000", "000600")
    }

    /** This preview advanced one tick at a time until its T has locked. */
    private fun PreviewReplay.untilLocked(): PreviewReplay =
        generateSequence(this) { it.advancedBy(1) }.take(MAX_TICKS).first { it.piece == null }
}

/** More ticks than a T takes to fall and lock at level 0, the slowest. */
private const val MAX_TICKS = 1_000

/** How long the lines show cleared, in ticks: 2 seconds. */
private const val SHOWN_CLEARED = 120
