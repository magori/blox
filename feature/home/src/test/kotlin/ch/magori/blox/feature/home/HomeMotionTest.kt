package ch.magori.blox.feature.home

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
}

private const val SECOND = 60
