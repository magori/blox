package ch.magori.blox.core.audio

import app.cash.turbine.test
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Volume
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The contract of [Player] and [Playback], what every player does, the real one and the fake shared by
 * the tests of the features alike: which score is heard, at which tempo, which tempos are ready ahead,
 * at which volume, and whether a playback still plays.
 * A subclass gives a player and tells what is heard of it; what is proper to a player (its sinks, its
 * renderings, the place it resumes from) is tested in the subclass.
 */
abstract class MusicPlayerContract {
    /** What is heard of a player: the score playing, its tempo factor, and the gain of its volume. */
    protected data class Heard(val score: ScoreId, val tempo: Double, val gain: Float = Volume.FULL.gain)

    /** A player under test, and what is heard of it. */
    protected interface PlayerUnderTest {
        val player: Player<Score>

        /** What is heard now, null in silence, once the work due has run. */
        suspend fun heard(): Heard?

        /**
         * The tempo factors other than 1.0 ready ahead for [score], once the work due has run: for the
         * sequencer, those rendered at least once for it; for the fake, those prepared for the score playing.
         */
        fun prepared(score: ScoreId): Set<Double>

        /**
         * Makes the music playing stop by itself, as an audio output that fails, once the work due has run:
         * for the sequencer, a write refused; for the fake, [FakeMusicPlayer.fail].
         */
        suspend fun fail()
    }

    /** A new player, whatever it needs to live in [scope] (the sequencer does). */
    protected abstract fun playerUnderTest(scope: TestScope): PlayerUnderTest

    /**
     * [player] heard through [fake], which it plays on, or is: the score the fake plays, at its tempo and volume, and
     * the tempos prepared for it.
     */
    protected fun heardThrough(
        fake: FakeMusicPlayer,
        player: Player<Score> = fake,
    ): PlayerUnderTest = object : PlayerUnderTest {
        override val player: Player<Score> = player

        override suspend fun heard(): Heard? = fake.playing?.let { Heard(it, fake.tempo, fake.volume.gain) }

        override fun prepared(score: ScoreId): Set<Double> = if (fake.playing == score) fake.prepared else emptySet()

        override suspend fun fail() = fake.fail()
    }

    private val korobeiniki = scoreOf(ScoreId.KOROBEINIKI, Pitch.parse("E5"))
    private val minuet = scoreOf(ScoreId.MINUET, Pitch.parse("D5"))
    private val third = scoreOf(THIRD, Pitch.parse("C5"))

    @Test
    fun `play plays the score at the normal tempo`() = runTest {
        // Given
        val subject = playerUnderTest(this)

        // When
        subject.player.play(korobeiniki)

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0))
    }

    @Test
    fun `stopping the playback silences its score`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()

        // When
        playback.stop()

        // Then
        assertThat(subject.heard()).isNull()
    }

    @Test
    fun `a new play replaces the score playing`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        subject.player.play(korobeiniki)
        subject.heard()

        // When
        subject.player.play(minuet)

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.MINUET, tempo = 1.0))
    }

    @Test
    fun `stopping a playback that was replaced leaves the new one playing`() = runTest {
        // Given a score playing, replaced by another one
        val subject = playerUnderTest(this)
        val first = subject.player.play(korobeiniki)
        subject.heard()
        subject.player.play(minuet)
        subject.heard()

        // When the first playback is stopped, as its game is left after the next one started
        first.stop()

        // Then the new one goes on
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.MINUET, tempo = 1.0))
    }

    @Test
    fun `a stopped playback resumes its score`() = runTest {
        // Given a score stopped, as a game paused
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()
        playback.stop()
        subject.heard()

        // When
        playback.resume()

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0))
    }

    @Test
    fun `stopping a playback twice, then resuming it, plays its score`() = runTest {
        // Given a score stopped twice, as a game paused, then left
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()
        playback.stop()
        playback.stop()
        subject.heard()

        // When
        playback.resume()

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0))
    }

    @Test
    fun `stopping a playback again once resumed leaves its resumption playing`() = runTest {
        // Given a score stopped, then resumed, as a game paused then resumed
        val subject = playerUnderTest(this)
        val first = subject.player.play(korobeiniki)
        subject.heard()
        first.stop()
        first.resume()
        subject.heard()

        // When the first playback, replaced by its resumption, is stopped again
        first.stop()

        // Then the resumption goes on
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0))
    }

    @Test
    fun `a playback resumed while another score plays replaces it`() = runTest {
        // Given a score stopped, then another one playing
        val subject = playerUnderTest(this)
        val first = subject.player.play(korobeiniki)
        subject.heard()
        first.stop()
        subject.player.play(minuet)
        subject.heard()

        // When
        first.resume()

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0))
    }

    @Test
    fun `a playback plays as soon as its score is played`() = runTest {
        // Given
        val subject = playerUnderTest(this)

        // When
        val playback = subject.player.play(korobeiniki)

        // Then
        assertThat(playback.isPlaying.first()).isTrue()
        subject.heard()
        assertThat(playback.isPlaying.first()).isTrue()
    }

    @Test
    fun `a stopped playback no longer plays`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()

        // When
        playback.stop()

        // Then
        assertThat(playback.isPlaying.first()).isFalse()
    }

    @Test
    fun `a playback replaced by another play no longer plays, the new one does`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        val first = subject.player.play(korobeiniki)
        subject.heard()

        // When
        val second = subject.player.play(minuet)

        // Then
        assertThat(first.isPlaying.first()).isFalse()
        assertThat(second.isPlaying.first()).isTrue()
    }

    @Test
    fun `a resumed playback plays, the playback it resumes stays stopped`() = runTest {
        // Given a score stopped, as a game paused
        val subject = playerUnderTest(this)
        val first = subject.player.play(korobeiniki)
        subject.heard()
        first.stop()

        // When
        val resumed = first.resume()

        // Then
        assertThat(resumed.isPlaying.first()).isTrue()
        assertThat(first.isPlaying.first()).isFalse()
    }

    @Test
    fun `a playback whose music stops by itself, as the audio output fails, no longer plays`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()

        // When the music stops by itself
        subject.fail()

        // Then its playback says so, and nothing is heard
        assertThat(playback.isPlaying.first()).isFalse()
        assertThat(subject.heard()).isNull()
    }

    @Test
    fun `the playing state of a playback follows it, from its start to its end`() = runTest {
        // Given a score playing, its state followed
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()

        playback.isPlaying.test {
            assertThat(awaitItem()).isTrue()

            // When the music stops by itself
            subject.fail()

            // Then the end is told once, and nothing more
            assertThat(awaitItem()).isFalse()
            subject.heard()
            expectNoEvents()
        }
    }

    @Test
    fun `the score playing goes on at the tempo factor set`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        subject.player.play(korobeiniki)
        subject.heard()

        // When the stack comes near the top
        subject.player.setTempoFactor(1.25)

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.25))
    }

    @Test
    fun `a new play starts at the normal tempo, though the music played before was faster`() = runTest {
        // Given a score sped up (a game in danger), then stopped (the game over)
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.player.setTempoFactor(1.25)
        subject.heard()
        playback.stop()
        subject.heard()

        // When a score is played again (the preview of the home screen, a new game)
        subject.player.play(minuet)

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.MINUET, tempo = 1.0))
    }

    @Test
    fun `a playback stopped at a faster tempo resumes at that tempo`() = runTest {
        // Given a score sped up (a game in danger), then stopped (the game paused)
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.player.setTempoFactor(1.25)
        subject.heard()
        playback.stop()
        subject.heard()

        // When
        playback.resume()

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.25))
    }

    @Test
    fun `a tempo factor set while nothing plays is kept for the next resume`() = runTest {
        // Given a score stopped (the game paused)
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()
        playback.stop()
        subject.heard()

        // When the tempo changes meanwhile, then the score resumes
        subject.player.setTempoFactor(1.25)
        playback.resume()

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.25))
    }

    @Test
    fun `a tempo factor set while nothing plays is not kept for the next play`() = runTest {
        // Given
        val subject = playerUnderTest(this)
        subject.player.setTempoFactor(1.25)

        // When
        subject.player.play(korobeiniki)

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0))
    }

    @Test
    fun `a tempo prepared is ready for the score playing`() = runTest {
        // Given
        val subject = playerUnderTest(this)

        // When a score plays, its danger tempo prepared, as a game does
        subject.player.play(korobeiniki)
        subject.player.prepareTempo(1.25)

        // Then
        assertThat(subject.prepared(ScoreId.KOROBEINIKI)).containsExactly(1.25)
    }

    @Test
    fun `a tempo prepared while the score plays is ready`() = runTest {
        // Given a score playing, no other tempo ready yet
        val subject = playerUnderTest(this)
        subject.player.play(korobeiniki)
        subject.heard()
        assertThat(subject.prepared(ScoreId.KOROBEINIKI)).isEmpty()

        // When its danger tempo is prepared
        subject.player.prepareTempo(1.25)

        // Then
        assertThat(subject.prepared(ScoreId.KOROBEINIKI)).containsExactly(1.25)
    }

    @Test
    fun `a tempo prepared while nothing plays is kept for the next resume`() = runTest {
        // Given a score stopped (the game paused)
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()
        playback.stop()
        subject.heard()

        // When a tempo is prepared meanwhile, then the score resumes
        subject.player.prepareTempo(1.25)
        playback.resume()

        // Then
        assertThat(subject.prepared(ScoreId.KOROBEINIKI)).containsExactly(1.25)
    }

    @Test
    fun `a new play forgets the tempos prepared before, as a preview prepares no other tempo`() = runTest {
        // Given a score played by a game, its danger tempo prepared
        val subject = playerUnderTest(this)
        subject.player.play(korobeiniki)
        subject.player.prepareTempo(1.25)

        // When another score plays, as the preview of the home screen
        subject.player.play(minuet)

        // Then
        assertThat(subject.prepared(ScoreId.MINUET)).isEmpty()
    }

    @Test
    fun `a resume keeps the tempos prepared`() = runTest {
        // Given a score played by a game, its danger tempo prepared, stopped at once (the game paused)
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.player.prepareTempo(1.25)
        playback.stop()

        // When the game resumes
        playback.resume()

        // Then
        assertThat(subject.prepared(ScoreId.KOROBEINIKI)).containsExactly(1.25)
    }

    @Test
    fun `a switch keeps the tempo factor and the playback`() = runTest {
        // Given a score playing faster, as a game in danger
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.player.setTempoFactor(1.25)
        subject.heard()

        // When another score is switched to, as the live music turns tense
        subject.player.switchTo(minuet)

        // Then it plays at the same tempo, under the same playback, which stops it
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.MINUET, tempo = 1.25))
        assertThat(playback.isPlaying.first()).isTrue()
        playback.stop()
        assertThat(subject.heard()).isNull()
    }

    @Test
    fun `a second switch before the bar line replaces the first`() = runTest {
        // Given a score playing, another one switched to, its bar line not reached yet
        val subject = playerUnderTest(this)
        subject.player.play(korobeiniki)
        subject.heard()
        subject.player.switchTo(minuet)

        // When a third score is switched to
        subject.player.switchTo(third)

        // Then only the last one plays
        assertThat(subject.heard()).isEqualTo(Heard(THIRD, tempo = 1.0))
    }

    @Test
    fun `a switch while the music is stopped is played at the resume`() = runTest {
        // Given a score stopped, as a game paused
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()
        playback.stop()

        // When another score is switched to meanwhile
        subject.player.switchTo(minuet)

        // Then nothing plays until the score switched to resumes
        assertThat(subject.heard()).isNull()
        playback.resume()
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.MINUET, tempo = 1.0))
    }

    @Test
    fun `a switch while the music stopped by itself is played at the resume`() = runTest {
        // Given a score whose music stopped by itself, as the audio output fails
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()
        subject.fail()

        // When another score is switched to meanwhile, then the game is paused and resumed
        subject.player.switchTo(minuet)
        playback.stop()
        playback.resume()

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.MINUET, tempo = 1.0))
    }

    @Test
    fun `a switch before anything is played plays nothing`() = runTest {
        // Given
        val subject = playerUnderTest(this)

        // When
        subject.player.switchTo(minuet)

        // Then
        assertThat(subject.heard()).isNull()
    }

    @Test
    fun `the tempos prepared are ready for the score switched to`() = runTest {
        // Given a score playing, its danger tempo prepared, as a game does
        val subject = playerUnderTest(this)
        subject.player.play(korobeiniki)
        subject.player.prepareTempo(1.25)
        subject.heard()

        // When another score is switched to
        subject.player.switchTo(minuet)

        // Then its danger tempo is ready too
        assertThat(subject.prepared(ScoreId.MINUET)).containsExactly(1.25)
    }

    @Test
    fun `a volume set before a play applies to it`() = runTest {
        // Given the volume of the settings, applied as the application starts
        val subject = playerUnderTest(this)
        subject.player.setVolume(Volume(40))

        // When a score plays, as the preview of the home screen
        subject.player.play(korobeiniki)

        // Then
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0, gain = Volume(40).gain))
    }

    @Test
    fun `a volume set while a score plays applies to it, down to silence, without stopping it`() = runTest {
        // Given a score playing
        val subject = playerUnderTest(this)
        val playback = subject.player.play(korobeiniki)
        subject.heard()

        // When the player turns the music down to 0, from the settings
        subject.player.setVolume(Volume.SILENT)

        // Then it plays on, in silence
        assertThat(subject.heard()).isEqualTo(Heard(ScoreId.KOROBEINIKI, tempo = 1.0, gain = 0f))
        assertThat(playback.isPlaying.first()).isTrue()
    }

    @ParameterizedTest(name = "a tempo factor of {0} is refused")
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `the tempo factor must be positive and finite`(factor: Double) = runTest {
        // Given
        val player = playerUnderTest(this).player

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { player.setTempoFactor(factor) }
    }

    @ParameterizedTest(name = "a tempo factor of {0} prepared is refused")
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `the tempo factor prepared must be positive and finite`(factor: Double) = runTest {
        // Given
        val player = playerUnderTest(this).player

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { player.prepareTempo(factor) }
    }

    /** A score of four quarter notes: the tempo may switch on every beat. */
    private fun scoreOf(
        id: ScoreId,
        pitch: Pitch,
    ): Score = Score(
        id = id,
        bpm = 150,
        beatsPerBar = 4,
        voices = listOf(Voice(Waveform.SQUARE, volume = 0.07, notes = List(4) { Note(pitch, beats = 1.0) })),
    )

    companion object {
        /** A third score, generated or written by the player: the live music switches between more than two. */
        val THIRD: ScoreId = ScoreId.Personal("third")
    }
}
