package ch.magori.blox.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Encodes samples from −1 to 1 as a WAV file: a RIFF `WAVE` header, a `fmt ` chunk of PCM, mono, 16 bits, then
 * the `data` chunk, little endian (ADR 0031). A sample of 1 is written as 32 767, of −1 as −32 767; beyond, it is
 * clipped. A sample is rounded to the nearest step, a half step up: −0.5 step gives 0, +0.5 gives 1, which is not
 * heard.
 */
internal object WavEncoder {
    fun encode(
        samples: FloatArray,
        sampleRate: Int,
    ): ByteArray {
        require(samples.isNotEmpty()) { "A WAV file holds at least one sample" }
        val dataBytes = samples.size * BYTES_PER_SAMPLE
        val wav = ByteBuffer.allocate(HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        wav.putTag("RIFF").putInt(HEADER_BYTES - RIFF_PREAMBLE_BYTES + dataBytes)
        wav.putTag("WAVE")
        wav.putTag("fmt ").putInt(FMT_CHUNK_BYTES)
        wav.putShort(PCM).putShort(MONO).putInt(sampleRate).putInt(sampleRate * BYTES_PER_SAMPLE)
        wav.putShort(BYTES_PER_SAMPLE.toShort()).putShort(BITS_PER_SAMPLE)
        wav.putTag("data").putInt(dataBytes)
        for (sample in samples) wav.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort())
        return wav.array()
    }

    private fun ByteBuffer.putTag(tag: String): ByteBuffer = put(tag.toByteArray(Charsets.US_ASCII))

    private const val HEADER_BYTES = 44

    /** The tag "RIFF" and the size that follows it, which the RIFF size leaves out. */
    private const val RIFF_PREAMBLE_BYTES = 8
    private const val FMT_CHUNK_BYTES = 16
    private const val PCM: Short = 1
    private const val MONO: Short = 1
    private const val BYTES_PER_SAMPLE = 2
    private const val BITS_PER_SAMPLE: Short = 16
}
