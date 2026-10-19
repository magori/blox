package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.rules.LevelProgression
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class GameProgressTest {
    @Test
    fun `the level is the one the level progression gives for the start level and the lines`() {
        // Given a progression whose level is the start level plus the lines
        val progression = progression { start, lines -> Level(start.value + lines.value) }

        // When
        val progress = GameProgress.of(startLevel = Level(2), lines = Lines(5), levelProgression = progression)

        // Then
        assertThat(progress.startLevel).isEqualTo(Level(2))
        assertThat(progress.lines).isEqualTo(Lines(5))
        assertThat(progress.level).isEqualTo(Level(7))
    }

    @Test
    fun `a progression that gives a level below the start level is refused`() {
        // Given a progression that always gives level 0
        val progression = progression { _, _ -> Level(0) }

        // When / Then
        assertThatIllegalArgumentException()
            .isThrownBy { GameProgress.of(startLevel = Level(3), lines = Lines(0), levelProgression = progression) }
            .withMessage("The level cannot be below the start level 3: 0")
    }

    @Test
    fun `a clear with a progression whose level goes down is refused`() {
        // Given a progression that gives level 1 below 2 lines and the start level from then on
        val progression = progression { start, lines -> if (lines.value >= 2) start else Level(start.value + 1) }
        val rules = ClassicRules.copy(levelProgression = progression)
        val progress = GameProgress.of(startLevel = Level(0), lines = Lines(0), levelProgression = progression)

        // When / Then
        assertThatIllegalArgumentException()
            .isThrownBy { progress.afterClear(LineClear(2), rules) }
            .withMessage("The level cannot go down after a clear: from 1 to 0")
    }

    /** A level progression that accepts the starts 0 to 9 and gives the level of [levelFor]. */
    private fun progression(levelFor: (Level, Lines) -> Level): LevelProgression = object : LevelProgression {
        override val startLevels: List<Level> = (0..9).map(::Level)

        override fun levelFor(
            start: Level,
            lines: Lines,
        ): Level = levelFor(start, lines)
    }
}
