package ch.magori.blox.feature.game.music

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.music.LiveMusic
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LiveMusicDirectionTest {
    private val player = FakeMusicPlayer()

    private val direction = LiveMusicDirection(live, startLevel = Level(0))

    @Test
    fun `a live game starts on the calm music`() {
        // Given the calm music played, as the game plays it at its start
        player.play(live.calm.score)

        // When
        direction.onStart(player)

        // Then
        assertThat(player.calls).containsExactly("play ${LiveMusic.CALM_ID}")
        assertThat(player.playing).isEqualTo(LiveMusic.CALM_ID)
        assertThat(player.tempo).isEqualTo(1.0)
    }

    @Test
    fun `entering the danger switches to the tense music`() {
        // Given a live game started on the calm music
        started()

        // When
        direction.onEvents(listOf(GameEvent.StackDanger(on = true)), player)

        // Then the tense music takes over, a quarter faster, the tempo given first so that it is rendered at it only
        assertThat(player.calls).containsExactly("tempo 1.25", "switch ${LiveMusic.TENSE_ID}")
        assertThat(player.playing).isEqualTo(LiveMusic.TENSE_ID)
    }

    @Test
    fun `leaving the danger switches back to the calm music`() {
        // Given a live game in danger, on the tense music
        started()
        direction.onEvents(listOf(GameEvent.StackDanger(on = true)), player)
        player.calls.clear()

        // When
        direction.onEvents(listOf(GameEvent.StackDanger(on = false)), player)

        // Then the calm music takes over, at its tempo
        assertThat(player.calls).containsExactly("tempo 1.0", "switch ${LiveMusic.CALM_ID}")
        assertThat(player.playing).isEqualTo(LiveMusic.CALM_ID)
    }

    @ParameterizedTest(name = "from level {0} to level {1}: tempo {2}")
    @CsvSource("0, 1, 1.02", "0, 5, 1.10", "0, 15, 1.30", "0, 16, 1.30", "0, 29, 1.30", "9, 10, 1.02", "18, 20, 1.04")
    fun `each level up speeds the music up by 2 percent, up to 1 point 3`(
        start: Int,
        reached: Int,
        expectedFactor: Double,
    ) {
        // Given a live game started at level [start], on the calm music
        val direction = LiveMusicDirection(live, startLevel = Level(start))
        player.play(live.calm.score)
        direction.onStart(player)

        // When it goes up level after level, to [reached]
        ((start + 1)..reached).forEach { direction.onEvents(listOf(GameEvent.LevelUp(Level(it))), player) }

        // Then the levels gained since the start speed the calm music up, without compounding
        assertThat(player.tempo).isCloseTo(expectedFactor, within(PRECISION))
        assertThat(player.playing).isEqualTo(LiveMusic.CALM_ID)
    }

    @Test
    fun `the speed of the levels and the speed of the danger multiply`() {
        // Given a live game started at level 0, gone up to level 2: 4 percent faster
        started()
        direction.onEvents(listOf(GameEvent.LevelUp(Level(1)), GameEvent.LevelUp(Level(2))), player)

        // When the stack enters the danger, then the game goes up to level 3 in it, then the stack leaves it
        direction.onEvents(listOf(GameEvent.StackDanger(on = true)), player)
        val enteringTheDanger = player.tempo
        direction.onEvents(listOf(GameEvent.LevelUp(Level(3))), player)
        val levelUpInTheDanger = player.tempo
        direction.onEvents(listOf(GameEvent.StackDanger(on = false)), player)

        // Then
        assertThat(enteringTheDanger).isCloseTo(1.04 * 1.25, within(PRECISION))
        assertThat(levelUpInTheDanger).isCloseTo(1.06 * 1.25, within(PRECISION))
        assertThat(player.tempo).isCloseTo(1.06, within(PRECISION))
    }

    @Test
    fun `a level up and the end of the danger on the same tick give one tempo, then the switch`() {
        // Given a live game in danger, on the tense music
        started()
        direction.onEvents(listOf(GameEvent.StackDanger(on = true)), player)
        player.calls.clear()

        // When one tick clears lines that give a level and bring the stack out of the danger
        direction.onEvents(listOf(GameEvent.LevelUp(Level(1)), GameEvent.StackDanger(on = false)), player)

        // Then the calm music takes over at the speed of the new level, rendered once
        assertThat(player.calls).containsExactly("tempo 1.02", "switch ${LiveMusic.CALM_ID}")
    }

    @Test
    fun `a game already in danger when its music starts plays the tense music, a quarter faster`() {
        // Given a live game whose stack entered the danger before its music started (composed late, or the game paused
        // before it started)
        direction.onEvents(listOf(GameEvent.StackDanger(on = true)), player)

        // When its music starts, from the calm one as every game
        player.play(live.calm.score)
        direction.onStart(player)

        // Then
        assertThat(player.playing).isEqualTo(LiveMusic.TENSE_ID)
        assertThat(player.tempo).isEqualTo(1.25)
    }

    @Test
    fun `a game gone up a level when its music starts plays the calm music at the speed of its level`() {
        // Given a live game gone up a level before its music started
        direction.onEvents(listOf(GameEvent.LevelUp(Level(1))), player)

        // When its music starts
        player.play(live.calm.score)
        direction.onStart(player)

        // Then
        assertThat(player.playing).isEqualTo(LiveMusic.CALM_ID)
        assertThat(player.tempo).isCloseTo(1.02, within(PRECISION))
    }

    @Test
    fun `a direction built after a level up starts at the speed of its level`() {
        // Given the direction of a game started at level 3, built once it reached level 5 (its music composed late)
        val direction = LiveMusicDirection(live, startLevel = Level(3), level = Level(5))

        // When its music starts
        player.play(live.calm.score)
        direction.onStart(player)

        // Then the two levels gained speed it up
        assertThat(player.playing).isEqualTo(LiveMusic.CALM_ID)
        assertThat(player.tempo).isCloseTo(1.04, within(PRECISION))
    }

    /** The calm music played and the direction started, as at the start of a game; what the player noted, forgotten. */
    private fun started() {
        player.play(live.calm.score)
        direction.onStart(player)
        player.calls.clear()
    }

    private companion object {
        /** The two moods of a game, composed once for every test. */
        val live: LiveMusic = LiveMusic.compose(seed = 1L)

        /** Tempo factors are computed: equal to within this much. */
        const val PRECISION: Double = 1e-9
    }
}
