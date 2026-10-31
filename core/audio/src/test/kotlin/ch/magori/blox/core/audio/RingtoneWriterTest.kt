package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIOException
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RingtoneWriterTest {
    // A loop of 4 beats at 120 bpm, a bar of 4/4: 2 s, 16 000 samples at 8 000 Hz.
    private val score = Score(
        ScoreId.Personal("ringtone"),
        bpm = 120,
        beatsPerBar = 4,
        voices = listOf(Voice(Waveform.SQUARE, volume = 0.1, notes = List(4) { Note(Pitch.parse("A4"), beats = 1.0) })),
    )

    private val synthesizer = Synthesizer(sampleRate = 8_000)

    @Test
    fun `the exporter renders the score with the synthesizer of the music, at its tempo`() {
        // Given
        val store = FakeRingtoneStore()

        // When
        val entry = store.write()

        // Then: a file at the rate of the synthesizer, of whole loops of 2 s, as the score lasts at its tempo
        val wav = ByteBuffer.wrap(store.entries.getValue(entry).bytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(wav.getInt(SAMPLE_RATE_OFFSET)).isEqualTo(8_000)
        assertThat(wav.getInt(DATA_SIZE_OFFSET) / BYTES_PER_SAMPLE % 16_000).isZero()
    }

    @Test
    fun `the file holds the ringtone samples of the rendering, encoded as WAV`() {
        // Given
        val store = FakeRingtoneStore()

        // When
        val entry = store.write()

        // Then: 10 loops of 2 s, brought to −1 dBFS, bars of 2 s
        val ringtone = ringtoneSamples(synthesizer.render(score), sampleRate = 8_000, samplesPerBar = 16_000.0)
        assertThat(store.entries.getValue(entry).bytes.toByteArray()).isEqualTo(WavEncoder.encode(ringtone, 8_000))
    }

    @Test
    fun `an entry is published only once its file is whole`() {
        // Given
        val store = FakeRingtoneStore()

        // When
        val entry = store.write()

        // Then: published with every byte of the file, its stream closed
        val written = store.entries.getValue(entry)
        assertThat(written.publishedBytes).isEqualTo(written.bytes.toByteArray())
        assertThat(written.closedWhenPublished).isTrue()
    }

    @Test
    fun `a write that fails leaves no entry behind`() {
        // Given: a storage that fails on the first byte
        val store = FakeRingtoneStore(failingWrites = true)

        // When
        val write = { store.write(RingtoneKind.ALARM) }

        // Then
        assertThatIOException().isThrownBy { write() }
        assertThat(store.entries).isEmpty()
    }

    /** Writes the score, named Ma musique, as [kind] into this store, and returns its entry. */
    private fun FakeRingtoneStore.write(kind: RingtoneKind = RingtoneKind.RINGTONE): Int =
        RingtoneWriter(synthesizer, this).write(score, "Ma musique", kind)

    private companion object {
        const val SAMPLE_RATE_OFFSET = 24
        const val DATA_SIZE_OFFSET = 40
        const val BYTES_PER_SAMPLE = 2
    }
}
