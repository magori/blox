package ch.magori.blox.core.audio

import app.cash.turbine.test
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FocusedMusicPlayerTest {
    private val player = FakeMusicPlayer()
    private val focus = FakeAudioFocus(log = player.calls)
    private val music = FocusedMusicPlayer(player, focus)

    @Test
    fun `playing requests the audio focus, then plays`() {
        // When
        music.play(korobeiniki)

        // Then
        assertThat(player.calls).containsExactly("focus request", "play KOROBEINIKI")
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `a focus refused plays nothing`() {
        // Given another app that keeps the focus, as a phone call
        focus.granted = false

        // When
        music.play(korobeiniki)

        // Then
        assertThat(player.calls).containsExactly("focus request")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `stopping abandons the audio focus`() {
        // Given a score playing
        val playback = music.play(korobeiniki)

        // When
        playback.stop()

        // Then
        assertThat(player.calls).containsExactly("focus request", "play KOROBEINIKI", "stop", "focus abandon")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `stopping a playback that was replaced keeps the focus`() {
        // Given a score playing, replaced by another one
        val first = music.play(korobeiniki)
        music.play(minuet)
        player.calls.clear()

        // When the first playback is stopped, as its game is left after the next one started
        first.stop()

        // Then the new one plays on, with the focus
        assertThat(player.calls).doesNotContain("focus abandon")
        assertThat(player.playing).isEqualTo(ScoreId.MINUET)
    }

    @Test
    fun `playing another score keeps the focus without asking for it again`() {
        // Given a score playing
        music.play(korobeiniki)

        // When another one is played in its place
        music.play(minuet)

        // Then
        assertThat(player.calls).containsExactly("focus request", "play KOROBEINIKI", "play MINUET")
        assertThat(player.playing).isEqualTo(ScoreId.MINUET)
    }

    @Test
    fun `resuming requests the audio focus again, then resumes`() {
        // Given a score stopped, as a game paused
        val playback = music.play(korobeiniki)
        playback.stop()
        player.calls.clear()

        // When
        playback.resume()

        // Then
        assertThat(player.calls).containsExactly("focus request", "resume KOROBEINIKI")
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `a playback refused the focus plays from its start once resumed with it`() {
        // Given a score refused the focus, as during a phone call
        focus.granted = false
        val playback = music.play(korobeiniki)
        focus.granted = true
        player.calls.clear()

        // When it is resumed once the call is over, as a game resumed
        playback.resume()

        // Then it plays from its start: it never played
        assertThat(player.calls).containsExactly("focus request", "play KOROBEINIKI")
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `losing the focus for good stops the music and abandons the focus`() {
        // Given a score playing
        music.play(korobeiniki)
        player.calls.clear()

        // When another app takes the focus for good, as a podcast started
        focus.send(FocusEvent.LOSS)

        // Then
        assertThat(player.calls).containsExactly("stop", "focus abandon")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `losing the focus for a while pauses the music, waiting for the focus to come back`() {
        // Given a score playing
        music.play(korobeiniki)
        player.calls.clear()

        // When another app takes the focus for a while, as a voice assistant
        focus.send(FocusEvent.TRANSIENT_LOSS)

        // Then the music stops, but the focus is kept, to hear when it comes back
        assertThat(player.calls).containsExactly("stop")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `getting the focus back resumes the music where it paused`() {
        // Given a score paused by a focus lost for a while
        music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)
        player.calls.clear()

        // When the focus comes back
        focus.send(FocusEvent.GAIN)

        // Then the music goes on
        assertThat(player.calls).containsExactly("resume KOROBEINIKI")
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `a playback resumed by the focus still gives it back when stopped`() {
        // Given a score paused by a focus lost for a while, then resumed by its return
        val playback = music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)
        focus.send(FocusEvent.GAIN)
        player.calls.clear()

        // When the game stops it
        playback.stop()

        // Then the resumed music stops, and the focus is given back
        assertThat(player.calls).containsExactly("stop", "focus abandon")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `playing another score while the focus is lost for a while waits for the focus to come back`() {
        // Given a score paused by a focus lost for a while
        music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)
        player.calls.clear()

        // When another score is played
        music.play(minuet)

        // Then nothing plays over the other app
        assertThat(player.calls).isEmpty()
        assertThat(player.playing).isNull()

        // When the focus comes back
        focus.send(FocusEvent.GAIN)

        // Then the new score plays
        assertThat(player.calls).containsExactly("play MINUET")
        assertThat(player.playing).isEqualTo(ScoreId.MINUET)
    }

    @Test
    fun `resuming while the focus is lost for a while waits for the focus to come back`() {
        // Given a score paused by a focus lost for a while
        val playback = music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)
        player.calls.clear()

        // When it is resumed, without being stopped first
        playback.resume()

        // Then nothing plays over the other app
        assertThat(player.calls).isEmpty()
        assertThat(player.playing).isNull()

        // When the focus comes back
        focus.send(FocusEvent.GAIN)

        // Then the score goes on, once
        assertThat(player.calls).containsExactly("resume KOROBEINIKI")
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `pausing the game while the focus is lost for a while gives it back, and its return resumes nothing`() {
        // Given a score paused by a focus lost for a while
        val playback = music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)
        player.calls.clear()

        // When the game is paused, then the focus comes back
        playback.stop()
        focus.send(FocusEvent.GAIN)

        // Then the focus is given back, and nothing plays
        assertThat(player.calls).containsExactly("focus abandon")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `a playback stopped by a loss of the focus asks for it again when the game resumes`() {
        // Given a score stopped by a focus lost for good, then by the game paused
        val playback = music.play(korobeiniki)
        focus.send(FocusEvent.LOSS)
        playback.stop()
        player.calls.clear()

        // When the game resumes
        playback.resume()

        // Then
        assertThat(player.calls).containsExactly("focus request", "resume KOROBEINIKI")
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `getting the focus while playing changes nothing`() {
        // Given a score playing, the system having ducked it for a notification, without a word
        music.play(korobeiniki)
        player.calls.clear()

        // When the system tells the focus is back
        focus.send(FocusEvent.GAIN)

        // Then the music plays on, not resumed from an older place
        assertThat(player.calls).isEmpty()
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    @Test
    fun `the headphones being unplugged stops the music and abandons the focus`() {
        // Given a score playing in headphones
        music.play(korobeiniki)
        player.calls.clear()

        // When they are unplugged: the music would go on through the speaker
        focus.send(FocusEvent.BECOMING_NOISY)

        // Then
        assertThat(player.calls).containsExactly("stop", "focus abandon")
        assertThat(player.playing).isNull()
    }

    @Test
    fun `setting the tempo, as a game without music does, asks for no focus`() {
        // When a game whose music is « Aucune » sets the tempo, and plays nothing
        music.setTempoFactor(1.5)

        // Then the tempo goes to the player, and the focus is left to the other apps
        assertThat(player.calls).containsExactly("tempo 1.5")
    }

    @Test
    fun `preparing a tempo asks for no focus, and goes to the player`() {
        // Given a score playing
        music.play(korobeiniki)
        player.calls.clear()

        // When a game prepares its danger tempo
        music.prepareTempo(1.25)

        // Then
        assertThat(player.prepared).containsExactly(1.25)
        assertThat(player.calls).isEmpty()
    }

    @Test
    fun `a tempo prepared while the focus is refused is prepared once the score plays`() {
        // Given a score refused the focus, its danger tempo prepared
        focus.granted = false
        val playback = music.play(korobeiniki)
        music.prepareTempo(1.25)

        // When it is resumed with the focus, and plays at last
        focus.granted = true
        playback.resume()

        // Then the player, which forgets the tempos prepared on play, has it prepared
        assertThat(player.playing).isEqualTo(ScoreId.KOROBEINIKI)
        assertThat(player.prepared).containsExactly(1.25)
    }

    @Test
    fun `a new play forgets the tempos prepared for the score refused the focus`() {
        // Given a score refused the focus, its danger tempo prepared
        focus.granted = false
        music.play(korobeiniki)
        music.prepareTempo(1.25)

        // When another score plays with the focus, as a preview
        focus.granted = true
        music.play(minuet)

        // Then
        assertThat(player.prepared).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["LOSS", "BECOMING_NOISY"])
    fun `a playback stopped by a loss of the focus or unplugged headphones no longer plays`(event: FocusEvent) =
        runTest {
            // Given a score playing
            val playback = music.play(korobeiniki)

            // When another app takes the focus for good, or the headphones are unplugged
            focus.send(event)

            // Then the home screen, which listens to it, may offer to listen again
            assertThat(playback.isPlaying.first()).isFalse()
        }

    @Test
    fun `a playback paused by a focus lost for a while still plays, waiting for the focus`() = runTest {
        // Given a score playing
        val playback = music.play(korobeiniki)

        // When another app takes the focus for a while, as a voice assistant
        focus.send(FocusEvent.TRANSIENT_LOSS)

        // Then the music comes back by itself with the focus: the playback is not over
        assertThat(playback.isPlaying.first()).isTrue()
    }

    @Test
    fun `a playback paused then resumed by the focus never tells it stopped`() = runTest {
        // Given a score playing, its state followed
        val playback = music.play(korobeiniki)

        playback.isPlaying.test {
            assertThat(awaitItem()).isTrue()

            // When the focus goes away for a while, then comes back
            focus.send(FocusEvent.TRANSIENT_LOSS)
            focus.send(FocusEvent.GAIN)

            // Then the home screen, which listens to it, keeps offering to stop it
            expectNoEvents()
        }
    }

    @Test
    fun `a stopped playback tells it stopped once, and nothing more`() = runTest {
        // Given a score playing, its state followed
        val playback = music.play(korobeiniki)

        playback.isPlaying.test {
            assertThat(awaitItem()).isTrue()

            // When
            playback.stop()

            // Then
            assertThat(awaitItem()).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun `a playback resumed by the focus that comes back plays, and follows the music resumed`() = runTest {
        // Given a score paused by a focus lost for a while, then back
        val playback = music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)
        focus.send(FocusEvent.GAIN)

        // When the music resumed stops by itself
        player.fail()

        // Then
        assertThat(playback.isPlaying.first()).isFalse()
    }

    @Test
    fun `a playback refused the focus does not play`() = runTest {
        // Given another app that keeps the focus, as a phone call
        focus.granted = false

        // When
        val playback = music.play(korobeiniki)

        // Then
        assertThat(playback.isPlaying.first()).isFalse()
    }

    @Test
    fun `a playback whose music stopped by itself never plays again with the focus going and coming back`() = runTest {
        // Given a score whose music stopped by itself, its state followed
        val playback = music.play(korobeiniki)

        playback.isPlaying.test {
            assertThat(awaitItem()).isTrue()
            player.fail()
            assertThat(awaitItem()).isFalse()

            // When the focus goes away for a while, then comes back
            focus.send(FocusEvent.TRANSIENT_LOSS)
            focus.send(FocusEvent.GAIN)

            // Then the playback stays over: the home screen keeps offering to listen again
            expectNoEvents()
        }
    }

    @Test
    fun `a playback waiting for the focus no longer plays once another score replaces it`() = runTest {
        // Given a score paused by a focus lost for a while
        val first = music.play(korobeiniki)
        focus.send(FocusEvent.TRANSIENT_LOSS)

        // When another score is played in its place, waiting for the focus too
        music.play(minuet)

        // Then
        assertThat(first.isPlaying.first()).isFalse()
    }

    private companion object {
        val note = Note(Pitch.parse("E5"), beats = 4.0)
        val korobeiniki = scoreOf(ScoreId.KOROBEINIKI)
        val minuet = scoreOf(ScoreId.MINUET)

        fun scoreOf(id: ScoreId): Score = Score(
            id = id,
            bpm = 150,
            beatsPerBar = 4,
            voices = listOf(Voice(Waveform.SQUARE, volume = 0.07, notes = listOf(note))),
        )
    }
}
