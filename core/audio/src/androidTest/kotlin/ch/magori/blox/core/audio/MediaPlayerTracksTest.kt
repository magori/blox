package ch.magori.blox.core.audio

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Volume
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * `MediaPlayer` really plays a music file of the device, in a loop, from the place asked for; what the player does
 * with its tracks is tested on the JVM (`AudioFilePlayerTest`). The files are short WAVs written by the test, so that
 * no audio file is kept in the repository.
 */
class MediaPlayerTracksTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val failures = mutableListOf<Exception>()
    private val tracks = MediaPlayerTracks(context, onFailure = { synchronized(failures) { failures += it } })
    private val opened = mutableListOf<MusicTrack>()

    @AfterEach
    fun releaseTracks() = opened.forEach(MusicTrack::release)

    @Test
    fun aFileOfTheDevicePlaysInALoopPastItsEnd() {
        // Given a WAV of a second and a half
        val file = wavFile(seconds = 1.5)

        // When it plays, once prepared, for four seconds
        val track = requireNotNull(tracks.open(file, startAt = 0, onError = {})).also(opened::add)
        waitUntil { track.position > 0 }
        val positions = sample(track, millis = 4_000)

        // Then its position went back at least once: past its end, it started again
        val wentBack = positions.zipWithNext().any { (before, after) -> after < before }
        assertThat(wentBack).withFailMessage { "The position never went back: $positions" }.isTrue()
        assertThat(failures).isEmpty()
    }

    @Test
    fun aFilePlaysFromThePlaceAskedFor() {
        // Given a WAV of six seconds
        val file = wavFile(seconds = 6.0)

        // When it is opened four seconds and a half in, as a resume
        val track = requireNotNull(tracks.open(file, startAt = 4_500, onError = {})).also(opened::add)

        // Then, once prepared, it plays on from there: it never goes back to the start, the position given before the
        // preparation being the place asked for itself
        val positions = mutableListOf<Int>()
        waitUntil { track.position.also(positions::add) > 4_800 }
        assertThat(positions).withFailMessage { "It did not play from 4.5 s: $positions" }.allMatch { it >= 4_400 }
        assertThat(failures).isEmpty()
    }

    @Test
    fun aFileTurnedDownWhileItPreparesThenToSilenceKeepsPlayingWithoutFailing() {
        // Given a WAV of a second and a half, turned down as soon as it is opened, while it prepares
        val file = wavFile(seconds = 1.5)
        val track = requireNotNull(tracks.open(file, startAt = 0, onError = {})).also(opened::add)
        track.setGain(Volume(50).gain)

        // When it plays, then is turned down to silence
        waitUntil { track.position > 0 }
        track.setGain(Volume.SILENT.gain)

        // Then it plays on, in silence, and nothing failed
        val positions = sample(track, millis = 500)
        assertThat(positions.last()).withFailMessage { "It stopped: $positions" }.isNotEqualTo(positions.first())
        assertThat(failures).isEmpty()
    }

    @Test
    fun aDocumentThatCannotBeOpenedFailsBeforeItStarts() {
        // Given a document no provider gives, as a file whose permission was lost
        val lost = MusicFile("content://ch.magori.blox.nothing/document/lost")
        val failedBeforeStart = mutableListOf<Boolean>()

        // When
        val track = tracks.open(lost, startAt = 0, onError = {
            synchronized(failedBeforeStart) { failedBeforeStart += it }
        })
            ?.also(opened::add)

        // Then it gives no track, or, as MediaPlayer may try the uri again as a path, it fails before it starts
        if (track != null) waitUntil { synchronized(failedBeforeStart) { failedBeforeStart.isNotEmpty() } }
        assertThat(failedBeforeStart).allMatch { it }
        assertThat(failures).isNotEmpty()
    }

    /** The positions of [track] every few milliseconds, for [millis]. */
    private fun sample(
        track: MusicTrack,
        millis: Long,
    ): List<Int> {
        val positions = mutableListOf<Int>()
        val end = System.nanoTime() + millis * 1_000_000
        while (System.nanoTime() < end) {
            positions += track.position
            Thread.sleep(POLL_MILLIS)
        }
        return positions
    }

    /**
     * Waits up to fifteen seconds (an emulator may take seconds to prepare) for [condition], which the main thread of
     * `MediaPlayer` makes true.
     */
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + WAIT_NANOS
        while (!condition()) {
            check(System.nanoTime() < deadline) { "The track never got there" }
            Thread.sleep(POLL_MILLIS)
        }
    }

    /** A WAV file of a tone lasting [seconds], 16 bits, mono, 8 kHz, in the cache of the test. */
    private fun wavFile(seconds: Double): MusicFile {
        val samples = (SAMPLE_RATE * seconds).toInt()
        val data = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(samples) { i -> data.putShort((sin(2 * PI * TONE_HZ * i / SAMPLE_RATE) * 8_000).toInt().toShort()) }
        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(WAV_HEADER_SIZE - 8 + samples * 2)
            put("WAVEfmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(samples * 2)
        }
        val file = File(context.cacheDir, "tone-$seconds.wav")
        file.writeBytes(header.array() + data.array())
        return MusicFile(Uri.fromFile(file).toString())
    }

    private companion object {
        const val SAMPLE_RATE = 8_000
        const val TONE_HZ = 440
        const val WAV_HEADER_SIZE = 44
        const val WAIT_NANOS = 15_000_000_000L
        const val POLL_MILLIS = 20L
    }
}
