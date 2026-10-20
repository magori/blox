package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SoundEffectsTest {
    @Test
    fun `the rotation is a single E6 of 30 ms on the square, the quietest effect, as it comes so often`() {
        // Given / When / Then
        assertThat(SoundEffects.rotation).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = 0.05,
                Envelope(attackMillis = 1, releaseMillis = 10),
                blips("E6" to 30),
            ),
        )
        assertThat(SoundEffects.all.minOf { it.volume }).isEqualTo(SoundEffects.rotation.volume)
    }

    @Test
    fun `the lines cleared rise up the chord of C, C6, E6, G6, in 160 ms on the square`() {
        // Given / When / Then
        assertThat(SoundEffects.linesCleared).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = 0.08,
                Envelope(attackMillis = 2, releaseMillis = 15),
                blips("C6" to 40, "E6" to 40, "G6" to 80),
            ),
        )
    }

    @Test
    fun `four lines cleared rise up the chord of C over two octaves, C5 to C7, in 450 ms on the square`() {
        // Given / When / Then
        assertThat(SoundEffects.fourLinesCleared).isEqualTo(
            SoundEffect(
                Waveform.SQUARE,
                volume = 0.09,
                Envelope(attackMillis = 2, releaseMillis = 15),
                blips("C5" to 50, "E5" to 50, "G5" to 50, "C6" to 50, "E6" to 50, "G6" to 50, "C7" to 150),
            ),
        )
    }

    @Test
    fun `the game over falls down the chord of C, G4, E4, C4, G3, in 600 ms on the triangle`() {
        // Given / When / Then
        assertThat(SoundEffects.gameOver).isEqualTo(
            SoundEffect(
                Waveform.TRIANGLE,
                volume = 0.2,
                Envelope(attackMillis = 2, releaseMillis = 30),
                blips("G4" to 120, "E4" to 120, "C4" to 120, "G3" to 240),
            ),
        )
    }

    @Test
    fun `every effect lasts from a few tens to a few hundred milliseconds`() {
        // Given / When / Then
        assertThat(SoundEffects.all.map { it.millis }).allSatisfy { assertThat(it).isBetween(20, 700) }
    }

    @Test
    fun `every blip rises and falls over a few milliseconds, so that none clicks`() {
        // Given / When / Then
        assertThat(SoundEffects.all.map { it.envelope }).allSatisfy { envelope ->
            assertThat(envelope.attackMillis).isPositive()
            assertThat(envelope.releaseMillis).isPositive()
        }
    }

    private fun blips(vararg blips: Pair<String, Int>): List<Blip> =
        blips.map { (pitch, millis) -> Blip(Pitch.parse(pitch), millis) }
}
