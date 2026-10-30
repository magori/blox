package ch.magori.blox.feature.home

import androidx.compose.runtime.snapshots.Snapshot
import ch.magori.blox.core.game.model.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomeMotionTest {
    @Test
    fun `the ticks of the screen play the preview at the level chosen and bring the halo of Jouer`() {
        // Given the home just shown at level 2
        val motion = HomeMotion(Level(2))

        // When 4 seconds go by, frame after frame
        repeat(4 * SECOND) { motion.advance(ticks = 1, level = Level(2)) }

        // Then the preview has played those 4 seconds, and the halo pulses
        assertThat(motion.replay).isEqualTo(PreviewReplay(Level(2)).advancedBy(4 * SECOND))
        assertThat(motion.halo).isEqualTo(playHalo(4 * SECOND))
        assertThat(motion.halo).isGreaterThan(0f)
    }

    @Test
    fun `the preview starts again with the new level when the level changes`() {
        // Given the preview playing at level 0
        val motion = HomeMotion(Level(0))
        motion.advance(ticks = 100, level = Level(0))

        // When the level 7 is chosen, then a tick goes by
        motion.advance(ticks = 1, level = Level(7))

        // Then the T came in again, at level 7
        assertThat(motion.replay).isEqualTo(PreviewReplay(Level(7)).advancedBy(1))
    }

    @Test
    fun `a touch on the home puts the halo off for 3 seconds again`() {
        // Given the halo pulsing, 3 and a half seconds after the home showed
        val motion = HomeMotion(Level(0))
        motion.advance(ticks = 3 * SECOND + SECOND / 2, level = Level(0))
        assertThat(motion.halo).isGreaterThan(0f)

        // When the player touches the home, then nearly 3 seconds go by
        motion.touched()
        motion.advance(ticks = 3 * SECOND - 1, level = Level(0))

        // Then no halo yet
        assertThat(motion.halo).isZero()
    }

    @Test
    fun `the preview notifies a change only when the T drops a row or locks`() {
        // Given the preview at level 9, a row every 6 ticks: 4 rows down to its hole, then it locks
        val motion = HomeMotion(Level(9))

        // When its first 30 ticks go by, one by one
        val changes = changesOf({ motion.preview }, ticks = 30) {
            motion.advance(ticks = 1, level = Level(9))
        }

        // Then 5 changes: 4 rows, then the lock, and nothing in between
        assertThat(changes).isEqualTo(5)
    }

    @Test
    fun `the halo notifies no change while it is off`() {
        // Given the home just shown
        val motion = HomeMotion(Level(0))

        // When its first 3 seconds go by, the halo off
        val changes = changesOf({ motion.halo }, ticks = 3 * SECOND - 1) {
            motion.advance(ticks = 1, level = Level(0))
        }

        // Then nothing to draw again for it
        assertThat(changes).isZero()
    }

    /**
     * How many of [ticks] ticks, each applied by [tick] in a snapshot of its own as a frame applies it, change what
     * [read] reads.
     */
    private fun changesOf(
        read: () -> Any,
        ticks: Int,
        tick: () -> Unit,
    ): Int {
        val reads = mutableSetOf<Any>()
        Snapshot.observe(readObserver = { reads += it }) { read() }
        var changes = 0
        val handle = Snapshot.registerApplyObserver { changed, _ -> if (changed.any { it in reads }) changes++ }
        try {
            repeat(ticks) { Snapshot.withMutableSnapshot(tick) }
        } finally {
            handle.dispose()
        }
        return changes
    }
}

private const val SECOND = 60
