package ch.magori.blox.feature.game.sound

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.spawned
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.music.SoundEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SoundEffectOfEventTest {
    @Test
    fun `a rotation gives the rotation effect`() {
        // When
        val effect = soundEffectFor(GameEvent.PieceRotated(spawned(PieceType.T), Rotation.CLOCKWISE))

        // Then
        assertThat(effect).isEqualTo(SoundEffects.rotation)
    }

    @ParameterizedTest(name = "{0} lines")
    @ValueSource(ints = [1, 2, 3])
    fun `one to three lines cleared give the lines cleared effect`(count: Int) {
        // When
        val effect = soundEffectFor(GameEvent.LinesCleared(LineClear(count)))

        // Then
        assertThat(effect).isEqualTo(SoundEffects.linesCleared)
    }

    @Test
    fun `four lines cleared at once give the four lines effect`() {
        // When
        val effect = soundEffectFor(GameEvent.LinesCleared(LineClear(4)))

        // Then
        assertThat(effect).isEqualTo(SoundEffects.fourLinesCleared)
    }

    @Test
    fun `the game over gives its own effect`() {
        // When
        val effect = soundEffectFor(GameEvent.GameOver)

        // Then
        assertThat(effect).isEqualTo(SoundEffects.gameOver)
    }

    @Test
    fun `a clear of no line, a lock and the danger give nothing`() {
        // Given
        val events = listOf(
            GameEvent.LinesCleared(LineClear(0)),
            GameEvent.PieceLocked(spawned(PieceType.O)),
            GameEvent.StackDanger(on = true),
            GameEvent.StackDanger(on = false),
        )

        // When
        val effects = events.map(::soundEffectFor)

        // Then
        assertThat(effects).containsOnlyNulls()
    }

    @Test
    fun `a level up gives the level up sound`() {
        // When
        val effect = soundEffectFor(GameEvent.LevelUp(Level(1)))

        // Then
        assertThat(effect).isEqualTo(SoundEffects.levelUp)
    }

    @ParameterizedTest(name = "{0} lines")
    @ValueSource(ints = [1, 2, 3])
    fun `on the tick of a level up the level up sound replaces the one to three lines effect`(count: Int) {
        // Given the events of the last tick of a clear that reaches the next level
        val events = listOf(GameEvent.LinesCleared(LineClear(count)), GameEvent.LevelUp(Level(1)))

        // When
        val effects = soundEffectsFor(events)

        // Then
        assertThat(effects).containsExactly(SoundEffects.levelUp)
    }

    @Test
    fun `four lines that reach the next level play the four lines effect alone`() {
        // Given
        val events = listOf(GameEvent.LinesCleared(LineClear(4)), GameEvent.LevelUp(Level(1)))

        // When
        val effects = soundEffectsFor(events)

        // Then
        assertThat(effects).containsExactly(SoundEffects.fourLinesCleared)
    }

    @Test
    fun `the other effects of a tick all play, in order, as the game over after a level up`() {
        // Given the last clear of a game that reaches the next level, then the next piece that does not fit
        val events = listOf(
            GameEvent.PieceLocked(spawned(PieceType.O)),
            GameEvent.LinesCleared(LineClear(1)),
            GameEvent.LevelUp(Level(1)),
            GameEvent.GameOver,
        )

        // When
        val effects = soundEffectsFor(events)

        // Then
        assertThat(effects).containsExactly(SoundEffects.levelUp, SoundEffects.gameOver)
    }

    @Test
    fun `a rotation on the tick of a level up still plays, before the level up sound`() {
        // Given
        val events = listOf(
            GameEvent.PieceRotated(spawned(PieceType.T), Rotation.CLOCKWISE),
            GameEvent.LinesCleared(LineClear(1)),
            GameEvent.LevelUp(Level(1)),
        )

        // When
        val effects = soundEffectsFor(events)

        // Then
        assertThat(effects).containsExactly(SoundEffects.rotation, SoundEffects.levelUp)
    }
}
