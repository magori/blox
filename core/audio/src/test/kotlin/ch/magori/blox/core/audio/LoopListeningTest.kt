package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** A score listened to in a loop on a screen (the music editor, the musical garden), until it stops. */
class LoopListeningTest {
    private val player = FakeMusicPlayer()
    private var stoppedByItself = 0

    private fun TestScope.listening(): LoopListening =
        LoopListening(player.play(Scores.korobeiniki), backgroundScope) { stoppedByItself++ }

    @Test
    fun `a score that stops by itself is told, once watched`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        listening().watch()

        // When the music stops by itself (the audio focus lost for good, the output failing)
        player.fail()

        // Then
        assertThat(stoppedByItself).isEqualTo(1)
    }

    @Test
    fun `a score refused at once is told as soon as it is watched, not before`() = runTest(UnconfinedTestDispatcher()) {
        // Given a playback that no longer plays as soon as it is given, as when the audio focus is refused
        val listening = listening()
        player.fail()
        val beforeWatching = stoppedByItself

        // When
        listening.watch()

        // Then
        assertThat(beforeWatching).isZero()
        assertThat(stoppedByItself).isEqualTo(1)
    }

    @Test
    fun `stop stops the score, and the score is no longer watched`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        val listening = listening().apply { watch() }

        // When
        listening.stop()

        // Then the playback stops, which the listening, no longer watched, does not tell as stopped by itself
        assertThat(player.calls).containsExactly("play KOROBEINIKI", "stop")
        assertThat(stoppedByItself).isZero()
    }
}
