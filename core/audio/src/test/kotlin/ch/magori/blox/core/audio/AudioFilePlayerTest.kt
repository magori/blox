package ch.magori.blox.core.audio

import app.cash.turbine.test
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

/** The logic of the player of music files, on fake tracks: what `MediaPlayer` does is left to [MediaPlayerTracks]. */
class AudioFilePlayerTest {
    private val myFile = MusicFile("content://music/my-file")
    private val otherFile = MusicFile("content://music/other")
    private val tracks = FakeTracks()
    private val player = AudioFilePlayer(tracks)

    /** How many times a file told it cannot be played. */
    private var unplayable = 0
    private val onUnplayable: () -> Unit = { unplayable++ }

    @Test
    fun `a file plays from its start`() = runTest {
        // When
        val playback = player.play(myFile, onUnplayable)

        // Then
        assertThat(tracks.opened).containsExactly("${myFile.uri} at 0")
        assertThat(playback?.isPlaying?.first()).isTrue()
    }

    @Test
    fun `a file that cannot be opened gives no playback, and the file playing goes on`() {
        // Given a file playing
        player.play(myFile, onUnplayable)
        tracks.unreadable += otherFile

        // When a file whose permission was lost is played
        val playback = player.play(otherFile, onUnplayable)

        // Then
        assertThat(playback).isNull()
        assertThat(tracks.live).containsExactly(myFile)
    }

    @Test
    fun `a new play replaces the file playing, whose playback no longer plays`() = runTest {
        // Given a file playing
        val first = player.play(myFile, onUnplayable)!!

        // When
        player.play(otherFile, onUnplayable)

        // Then
        assertThat(tracks.live).containsExactly(otherFile)
        assertThat(first.isPlaying.first()).isFalse()
    }

    @Test
    fun `a stopped playback releases its track and no longer plays`() = runTest {
        // Given
        val playback = player.play(myFile, onUnplayable)!!

        // When
        playback.stop()
        playback.stop()

        // Then
        assertThat(tracks.live).isEmpty()
        assertThat(tracks.released).isEqualTo(1)
        assertThat(playback.isPlaying.first()).isFalse()
    }

    @Test
    fun `stopping a playback that was replaced leaves the new one playing`() {
        // Given a file playing, replaced by another one
        val first = player.play(myFile, onUnplayable)!!
        player.play(otherFile, onUnplayable)

        // When
        first.stop()

        // Then
        assertThat(tracks.live).containsExactly(otherFile)
    }

    @Test
    fun `a stopped playback resumes the file from where it stopped`() = runTest {
        // Given a file stopped 12 seconds in, as a game paused
        val playback = player.play(myFile, onUnplayable)!!
        tracks.position = 12_000
        playback.stop()

        // When
        val resumed = playback.resume(onUnplayable)

        // Then
        assertThat(tracks.opened).containsExactly("${myFile.uri} at 0", "${myFile.uri} at 12000")
        assertThat(resumed?.isPlaying?.first()).isTrue()
    }

    @Test
    fun `a playback replaced by another file resumes from where it was replaced`() {
        // Given a file replaced 5 seconds in
        val first = player.play(myFile, onUnplayable)!!
        tracks.position = 5_000
        player.play(otherFile, onUnplayable)

        // When
        first.resume(onUnplayable)

        // Then
        assertThat(tracks.opened.last()).isEqualTo("${myFile.uri} at 5000")
        assertThat(tracks.live).containsExactly(myFile)
    }

    @Test
    fun `a track that fails while it plays ends its playback once, and its resume plays again from where it stood`() =
        runTest {
            // Given a file playing, its state followed
            val playback = player.play(myFile, onUnplayable)!!

            playback.isPlaying.test {
                assertThat(awaitItem()).isTrue()

                // When its track fails 7 seconds in (a format it cannot decode, a file gone while read)
                tracks.position = 7_000
                tracks.fail(beforeStart = false)

                // Then
                assertThat(awaitItem()).isFalse()
                assertThat(tracks.live).isEmpty()
                assertThat(unplayable).isZero()
                playback.stop()
                expectNoEvents()
            }
            playback.resume(onUnplayable)
            assertThat(tracks.opened.last()).isEqualTo("${myFile.uri} at 7000")
        }

    @Test
    fun `a track that fails before it starts tells the file cannot be played, then ends its playback`() = runTest {
        // Given a file that opens
        val playback = player.play(myFile, onUnplayable)!!

        // When its track fails before it starts, as a format the device cannot decode
        tracks.fail(beforeStart = true)

        // Then
        assertThat(unplayable).isEqualTo(1)
        assertThat(playback.isPlaying.first()).isFalse()
        assertThat(tracks.live).isEmpty()
    }

    @Test
    fun `a track that fails before it starts, once its playback was stopped, tells nothing`() {
        // Given a file stopped before its track started
        val playback = player.play(myFile, onUnplayable)!!
        playback.stop()

        // When the track would fail
        tracks.fail(beforeStart = true)

        // Then
        assertThat(unplayable).isZero()
    }

    @Test
    fun `a playback resumed once its file can no longer be opened gives no playback`() {
        // Given a file stopped, whose permission is then lost
        val playback = player.play(myFile, onUnplayable)!!
        playback.stop()
        tracks.unreadable += myFile

        // When
        val resumed = playback.resume(onUnplayable)

        // Then
        assertThat(resumed).isNull()
        assertThat(tracks.live).isEmpty()
    }

    @Test
    fun `a playback resumed twice plays once`() {
        // Given a file stopped
        val playback = player.play(myFile, onUnplayable)!!
        playback.stop()

        // When
        playback.resume(onUnplayable)
        playback.resume(onUnplayable)

        // Then
        assertThat(tracks.live).containsExactly(myFile)
    }

    @Test
    fun `a volume set applies to the file playing and to the files played after it`() {
        // Given a file playing
        player.play(myFile, onUnplayable)

        // When the music is turned down, then another file plays
        player.setVolume(Volume(50))
        player.play(otherFile, onUnplayable)

        // Then both play at the gain of that volume
        assertThat(tracks.gains).containsExactly(entry(myFile, Volume(50).gain), entry(otherFile, Volume(50).gain))
    }

    /** Tracks that note where they open and the gain they play at, and fail on demand. */
    private class FakeTracks : MusicTracks {
        val opened = mutableListOf<String>()
        val unreadable = mutableSetOf<MusicFile>()
        val live = mutableListOf<MusicFile>()
        var released = 0

        /** The gain the last track of each file played at: 1, that of a new track, until it is set. */
        val gains = mutableMapOf<MusicFile, Float>()

        /** The position of every track, in milliseconds. */
        var position = 0
        private var onError: ((Boolean) -> Unit)? = null

        override fun open(
            file: MusicFile,
            startAt: Int,
            onError: (beforeStart: Boolean) -> Unit,
        ): MusicTrack? {
            if (file in unreadable) return null
            opened += "${file.uri} at $startAt"
            live += file
            this.onError = onError
            gains[file] = 1f
            return object : MusicTrack {
                override val position: Int get() = this@FakeTracks.position

                override fun setGain(gain: Float) {
                    gains[file] = gain
                }

                override fun release() {
                    released++
                    live -= file
                }
            }
        }

        /** The last track opened fails, before it started or while it played, as `MediaPlayer` tells an error. */
        fun fail(beforeStart: Boolean) {
            onError?.invoke(beforeStart)
        }
    }
}
