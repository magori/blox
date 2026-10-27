package ch.magori.blox.feature.game.pause

import ch.magori.blox.core.designsystem.theme.BloxEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PauseCurtainStateTest {
    @Test
    fun `a game that pauses brings the curtain down`() {
        // Given a game under way
        val state = PauseCurtainState(paused = false, effects = BloxEffects.Full)

        // When it pauses
        state.follow(paused = true, effects = BloxEffects.Full)

        // Then
        assertThat(state.curtain?.phase).isEqualTo(CurtainPhase.CLOSING)
    }

    @Test
    fun `a game shown already paused has its curtain shut at once, its card shown`() {
        val state = PauseCurtainState(paused = true, effects = BloxEffects.Full)

        assertThat(state.curtain?.phase).isEqualTo(CurtainPhase.CLOSED)
    }

    @Test
    fun `Reprendre opens the curtain, and lets the game go on only once it is open`() {
        // Given a game paused, its card shown
        val state = PauseCurtainState(paused = true, effects = BloxEffects.Full)

        // When Reprendre opens the curtain, then 11 ticks pass
        state.resume()
        state.advance(ticks = 11)

        // Then the game waits, until the last tick
        assertThat(state.curtain?.phase).isEqualTo(CurtainPhase.OPENING)
        assertThat(state.gameMayGoOn).isFalse()
        state.advance(ticks = 1)
        assertThat(state.gameMayGoOn).isTrue()
    }

    @Test
    fun `a game that goes on, as when the view model resumes it, takes the curtain away`() {
        // Given a game paused, its curtain open
        val state = PauseCurtainState(paused = true, effects = BloxEffects.Full)
        state.resume()
        state.advance(ticks = 12)

        // When the game goes on
        state.follow(paused = false, effects = BloxEffects.Full)

        // Then
        assertThat(state.curtain).isNull()
        assertThat(state.gameMayGoOn).isFalse()
    }

    @Test
    fun `Reprendre before the curtain came lets the game go on at once`() {
        // Given a game paused this very frame: its curtain not there yet
        val state = PauseCurtainState(paused = false, effects = BloxEffects.Full)

        // When Reprendre comes (a key of a keyboard)
        state.resume()

        // Then
        assertThat(state.gameMayGoOn).isTrue()
    }

    @Test
    fun `a pause while the curtain opens brings it back down from where it stood, the game never let go on`() {
        // Given a curtain halfway up after Reprendre
        val state = PauseCurtainState(paused = true, effects = BloxEffects.Full)
        state.resume()
        state.advance(ticks = 6)

        // When the screen pauses again (ON_PAUSE, the system back, a window too small), then the curtain moves on
        state.pause()
        state.advance(ticks = 6)

        // Then it came back down from halfway, and is shut: the card shows again, the game still waits
        assertThat(state.curtain?.phase).isEqualTo(CurtainPhase.CLOSED)
    }

    @Test
    fun `the curtain moves while it comes down or goes up, and stays still once shut, once open or without one`() {
        // Given a game under way: no curtain
        val state = PauseCurtainState(paused = false, effects = BloxEffects.Full)
        assertThat(state.moving).isFalse()

        // When / Then: down, shut, up, open
        state.follow(paused = true, effects = BloxEffects.Full)
        assertThat(state.moving).isTrue()
        state.advance(ticks = 12)
        assertThat(state.moving).isFalse()
        state.resume()
        assertThat(state.moving).isTrue()
        state.advance(ticks = 12)
        assertThat(state.moving).isFalse()
    }
}
