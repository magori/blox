package ch.magori.blox.feature.game.music

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.music.Scores
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicMusicDirectionTest {
    private val player = FakeMusicPlayer()

    private val direction = ClassicMusicDirection()

    @ParameterizedTest(name = "stack in danger: {0} -> tempo {1}")
    @CsvSource("true, 1.25", "false, 1.0")
    fun `the music speeds up by a quarter while the stack is in danger, and goes back to its tempo after`(
        on: Boolean,
        expectedFactor: Double,
    ) {
        assertThat(ClassicMusicDirection.tempoFactorFor(GameEvent.StackDanger(on))).isEqualTo(expectedFactor)
    }

    @Test
    fun `the danger tempo is prepared as soon as the music starts`() {
        // When
        direction.onStart(player)

        // Then
        assertThat(player.prepared).containsExactly(1.25)
        assertThat(player.calls).isEmpty()
    }

    @Test
    fun `each change of danger of the game gives its tempo to the player, in order, and nothing else does`() {
        // When
        direction.onEvents(
            listOf(GameEvent.StackDanger(on = true), GameEvent.LevelUp(Level(1)), GameEvent.StackDanger(on = false)),
            player,
        )

        // Then
        assertThat(player.calls).containsExactly("tempo 1.25", "tempo 1.0")
        assertThat(player.prepared).isEmpty()
    }

    @Test
    fun `a game already in danger when its music starts plays a quarter faster`() {
        // Given a game whose stack entered the danger before its music started (read late, or the game paused before it
        // started)
        direction.onEvents(listOf(GameEvent.StackDanger(on = true)), player)

        // When its music starts, at the tempo of its score as every music played
        player.play(Scores.korobeiniki)
        direction.onStart(player)

        // Then
        assertThat(player.tempo).isEqualTo(1.25)
    }
}
