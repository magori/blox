package ch.magori.blox.core.audio

import app.cash.turbine.test
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What is proper to the switch: a score and a music file never play together, and an unreadable file falls back. */
class MusicSwitchTest {
    private val scores = FakeMusicPlayer()
    private val fileLog = mutableListOf<String>()
    private val myFile = MusicFile("content://music/my-file")
    private val lostFile = MusicFile("content://music/deleted")
    private val files = FakeMusicFilePlayer(fileLog, unreadable = mutableSetOf(lostFile))
    private val switch = MusicSwitch(scores = scores, files = files, fallback = Scores.korobeiniki)

    @Test
    fun `a score plays on the score player`() {
        // When
        switch.play(Scores.minuet)

        // Then
        assertThat(scores.playing).isEqualTo(Scores.minuet.id)
        assertThat(files.playing).isNull()
    }

    @Test
    fun `a music file plays on the file player`() {
        // When
        switch.play(myFile)

        // Then
        assertThat(files.playing).isEqualTo(myFile)
        assertThat(scores.playing).isNull()
    }

    @Test
    fun `a music file played while a score plays stops the score`() = runTest {
        // Given a score playing
        val score = switch.play(Scores.minuet)

        // When
        switch.play(myFile)

        // Then
        assertThat(scores.playing).isNull()
        assertThat(score.isPlaying.first()).isFalse()
        assertThat(files.playing).isEqualTo(myFile)
    }

    @Test
    fun `a score played while a music file plays stops the file`() = runTest {
        // Given a music file playing
        val file = switch.play(myFile)

        // When
        switch.play(Scores.minuet)

        // Then
        assertThat(files.playing).isNull()
        assertThat(file.isPlaying.first()).isFalse()
        assertThat(scores.playing).isEqualTo(Scores.minuet.id)
    }

    @Test
    fun `a music file that cannot be opened plays the fallback score in its place`() = runTest {
        // When a file whose permission was lost, or that was deleted, is played
        val playback = switch.play(lostFile)

        // Then Korobeiniki plays, and its playback is that of the fallback
        assertThat(scores.playing).isEqualTo(Scores.korobeiniki.id)
        assertThat(playback.isPlaying.first()).isTrue()
        playback.stop()
        assertThat(scores.playing).isNull()
    }

    @Test
    fun `a score resumed while a music file plays stops the file`() {
        // Given a score stopped (a game paused), then a music file playing (a preview)
        val score = switch.play(Scores.minuet)
        score.stop()
        switch.play(myFile)

        // When
        score.resume()

        // Then
        assertThat(files.playing).isNull()
        assertThat(scores.playing).isEqualTo(Scores.minuet.id)
    }

    @Test
    fun `a music file resumed while a score plays stops the score`() {
        // Given a music file stopped (a game paused), then a score playing
        val file = switch.play(myFile)
        file.stop()
        switch.play(Scores.minuet)

        // When
        file.resume()

        // Then
        assertThat(scores.playing).isNull()
        assertThat(files.playing).isEqualTo(myFile)
    }

    @Test
    fun `stopping a music file replaced by a score leaves the score playing`() {
        // Given a music file playing, replaced by a score (a new game after Rejouer)
        val file = switch.play(myFile)
        switch.play(Scores.minuet)

        // When the first one is stopped, as its game is left
        file.stop()

        // Then
        assertThat(scores.playing).isEqualTo(Scores.minuet.id)
    }

    @Test
    fun `the tempo goes to the scores, a music file plays at its own speed`() {
        // Given a music file playing
        switch.play(myFile)

        // When the stack enters the danger, and the game prepares its tempo
        switch.prepareTempo(1.25)
        switch.setTempoFactor(1.25)

        // Then the file goes on as it was: only the scores hear of the tempo
        assertThat(scores.prepared).containsExactly(1.25)
        assertThat(scores.tempo).isEqualTo(1.25)
        assertThat(fileLog).containsExactly("play file ${myFile.uri}")
    }

    @Test
    fun `a music file that opens but cannot be decoded plays the fallback score in its place`() = runTest {
        // Given a music file that opens, followed by a game
        val playback = switch.play(myFile)

        playback.isPlaying.test {
            assertThat(awaitItem()).isTrue()

            // When it fails before it starts, as a format the device cannot decode
            files.failBeforeStart()

            // Then Korobeiniki plays, through the same playback, which never tells it stopped
            assertThat(scores.playing).isEqualTo(Scores.korobeiniki.id)
            expectNoEvents()

            // When the game stops it
            playback.stop()

            // Then
            assertThat(awaitItem()).isFalse()
            assertThat(scores.playing).isNull()
        }
    }

    @Test
    fun `a music file that cannot be decoded resumes its fallback score`() {
        // Given a music file fallen back on Korobeiniki, then stopped (a game paused)
        val playback = switch.play(myFile)
        files.failBeforeStart()
        playback.stop()

        // When the game resumes
        playback.resume()

        // Then the fallback resumes, and the file is not tried again
        assertThat(scores.calls).containsExactly("play KOROBEINIKI", "stop", "resume KOROBEINIKI")
        assertThat(fileLog).containsExactly("play file ${myFile.uri}")
    }

    @Test
    fun `a music file that cannot be decoded once resumed plays the fallback score`() {
        // Given a music file stopped (a game paused), resumed, that then fails before it starts again
        val playback = switch.play(myFile)
        playback.stop()
        playback.resume()

        // When
        files.failBeforeStart()

        // Then
        assertThat(scores.playing).isEqualTo(Scores.korobeiniki.id)
    }

    @Test
    fun `a music file resumed once it can no longer be opened plays the fallback score`() = runTest {
        // Given a music file stopped (a game paused), whose permission is then lost
        val playback = switch.play(myFile)
        playback.stop()
        files.unreadable += myFile

        // When the game resumes
        val resumed = playback.resume()

        // Then
        assertThat(scores.playing).isEqualTo(Scores.korobeiniki.id)
        assertThat(resumed.isPlaying.first()).isTrue()
    }

    @Test
    fun `a music file that cannot be decoded after it was replaced plays nothing in place of the new music`() {
        // Given a music file replaced by a score
        switch.play(myFile)
        switch.play(Scores.minuet)

        // When the old file would tell it cannot be played, it has ended: the score goes on
        files.failBeforeStart()

        // Then
        assertThat(scores.playing).isEqualTo(Scores.minuet.id)
    }

    @Test
    fun `a music file played after a fallback stops the fallback score`() {
        // Given a music file fallen back on Korobeiniki
        switch.play(myFile)
        files.failBeforeStart()

        // When another file plays
        val other = MusicFile("content://music/other")
        switch.play(other)

        // Then
        assertThat(scores.playing).isNull()
        assertThat(files.playing).isEqualTo(other)
    }
}
