package ch.magori.blox.feature.game.sound

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.spawned
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.music.SoundEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SoundEffectOfEventTest {
    @Test
    fun `a rotation gives the rotation effect`() {
        // When
        val effect = soundEffectFor(GameEvent.PieceRotated(spawned(PieceType.T)))

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
    fun `a level up gives nothing yet`() {
        // When
        val effect = soundEffectFor(GameEvent.LevelUp(Level(1)))

        // Then
        assertThat(effect).isNull()
    }
}
