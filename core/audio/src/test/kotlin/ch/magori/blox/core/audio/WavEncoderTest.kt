package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavEncoderTest {
    @Test
    fun `the header is RIFF WAVE with a fmt chunk of PCM, mono, 16 bits, at the sample rate given`() {
        // When
        val wav = littleEndian(WavEncoder.encode(floatArrayOf(0f, 0.5f), sampleRate = 44_100))

        // Then
        assertThat(wav.ascii(at = 0)).isEqualTo("RIFF")
        assertThat(wav.ascii(at = 8)).isEqualTo("WAVE")
        assertThat(wav.ascii(at = 12)).isEqualTo("fmt ")
        assertThat(wav.getInt(16)).describedAs("size of the fmt chunk").isEqualTo(16)
        assertThat(wav.getShort(20)).describedAs("format, 1 for PCM").isEqualTo(1)
        assertThat(wav.getShort(22)).describedAs("channels").isEqualTo(1)
        assertThat(wav.getInt(24)).describedAs("sample rate").isEqualTo(44_100)
        assertThat(wav.getInt(28)).describedAs("bytes per second").isEqualTo(88_200)
        assertThat(wav.getShort(32)).describedAs("bytes per sample").isEqualTo(2)
        assertThat(wav.getShort(34)).describedAs("bits per sample").isEqualTo(16)
        assertThat(wav.ascii(at = 36)).isEqualTo("data")
    }

    @Test
    fun `the RIFF and data sizes match the number of samples`() {
        // When: 3 samples of 2 bytes
        val bytes = WavEncoder.encode(floatArrayOf(0f, 0.5f, -0.5f), sampleRate = 44_100)

        // Then
        val wav = littleEndian(bytes)
        assertThat(bytes).hasSize(44 + 6)
        assertThat(wav.getInt(4)).describedAs("RIFF size, the file after its first 8 bytes").isEqualTo(36 + 6)
        assertThat(wav.getInt(40)).describedAs("data size").isEqualTo(6)
    }

    @Test
    fun `a sample of 1 is written as 32767 and a sample of -1 as -32767, little endian`() {
        // When
        val bytes = WavEncoder.encode(floatArrayOf(1f, -1f), sampleRate = 44_100)

        // Then: 32 767 is 0x7FFF, −32 767 is 0x8001
        assertThat(bytes.copyOfRange(44, 48)).containsExactly(0xFF, 0x7F, 0x01, 0x80)
    }

    @Test
    fun `a sample beyond -1 or 1 is clipped`() {
        // When
        val wav = littleEndian(WavEncoder.encode(floatArrayOf(1.5f, -2f), sampleRate = 44_100))

        // Then
        assertThat(wav.getShort(44)).isEqualTo(Short.MAX_VALUE)
        assertThat(wav.getShort(46)).isEqualTo((-Short.MAX_VALUE).toShort())
    }

    @Test
    fun `zero is written as zero`() {
        // When
        val bytes = WavEncoder.encode(floatArrayOf(0f, -0f), sampleRate = 44_100)

        // Then
        assertThat(bytes.copyOfRange(44, 48)).containsOnly(0)
    }

    @Test
    fun `an empty rendering is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { WavEncoder.encode(floatArrayOf(), sampleRate = 44_100) }
    }

    private fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    private fun ByteBuffer.ascii(at: Int): String = String(ByteArray(4) { get(at + it) }, Charsets.US_ASCII)
}
