package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class MixTest {
    @Test
    fun `mixing adds the voices sample by sample`() {
        // Given
        val melody = floatArrayOf(0.5f, -0.5f, 0.25f, 0f)
        val bass = floatArrayOf(0.25f, -0.25f, -0.5f, 0f)

        // When
        val mixed = mix(listOf(melody, bass))

        // Then
        assertThat(mixed).containsExactly(0.75f, -0.75f, -0.25f, 0f)
    }

    @Test
    fun `the mix is clipped to -1 and 1`() {
        // Given
        val melody = floatArrayOf(0.75f, -0.75f)
        val bass = floatArrayOf(0.5f, -0.5f)

        // When
        val mixed = mix(listOf(melody, bass))

        // Then
        assertThat(mixed).containsExactly(1f, -1f)
    }

    @Test
    fun `mixing no track is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { mix(emptyList()) }
    }

    @Test
    fun `mixing tracks of different lengths is refused`() {
        // Given
        val short = floatArrayOf(0.5f, 0.5f)
        val long = floatArrayOf(0.25f, 0.25f, 0.25f)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { mix(listOf(short, long)) }
        assertThatIllegalArgumentException().isThrownBy { mix(listOf(long, short)) }
    }
}
