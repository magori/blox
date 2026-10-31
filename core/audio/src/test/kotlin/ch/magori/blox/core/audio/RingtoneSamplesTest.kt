package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RingtoneSamplesTest {
    @Test
    fun `a loop shorter than 20 seconds is repeated until the ringtone lasts at least 20 seconds`() {
        // Given: a loop of 7 s
        val loop = loopOf(seconds = 7)

        // When
        val ringtone = ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 10.0)

        // Then: 3 loops, 21 s
        assertThat(ringtone).hasSize(3 * loop.size)
    }

    @Test
    fun `the ringtone holds only whole loops, so that it loops without a seam`() {
        // Given: a loop of 6 s
        val loop = loopOf(seconds = 6)

        // When
        val ringtone = ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 10.0)

        // Then: 4 loops, each sample as loud, relative to the peak, as the one of the loop it repeats
        assertThat(ringtone).hasSize(4 * loop.size)
        assertSameShape(ringtone, FloatArray(ringtone.size) { loop[it % loop.size] })
    }

    @ParameterizedTest
    @ValueSource(ints = [20, 45, 60])
    fun `a loop of 20 to 60 seconds is kept once`(seconds: Int) {
        // Given
        val loop = loopOf(seconds)

        // When: bars of 7.3 samples, so that cutting a loop of 60 s would leave 82 bars, 599 samples
        val ringtone = ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 7.3)

        // Then
        assertThat(ringtone).hasSize(loop.size)
    }

    @Test
    fun `a loop longer than 60 seconds is cut at its last whole bar before 60 seconds`() {
        // Given: a loop of 70 s, whose bars last 7.3 samples, as a bar of a music rarely lasts a whole number of them
        val loop = loopOf(seconds = 70)

        // When
        val ringtone = ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 7.3)

        // Then: 82 bars end at 598.6 samples, on the nearest sample, as the synthesizer starts each note; 83 at 605.9
        assertThat(ringtone).hasSize(599)
        assertSameShape(ringtone, loop.copyOf(599))
    }

    @Test
    fun `the ringtone never lasts more than 60 seconds`() {
        // Given: loops of 1 to 130 s
        val loops = (1..130).map { loopOf(seconds = it) }

        // When
        val ringtones = loops.map { ringtoneSamples(it, SAMPLE_RATE, samplesPerBar = 7.3) }

        // Then
        ringtones.forEachIndexed { index, ringtone ->
            assertThat(ringtone.size).describedAs("a loop of ${index + 1} s").isLessThanOrEqualTo(60 * SAMPLE_RATE)
        }
    }

    @Test
    fun `the loudest sample is brought to minus 1 dBFS`() {
        // Given: a loop of 20 s whose loudest sample, -0.25, is negative
        val loop = FloatArray(20 * SAMPLE_RATE) { if (it == 7) -0.25f else 0.1f }

        // When
        val ringtone = ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 10.0)

        // Then: −1 dBFS is 10^(−1/20), and every sample is raised as much as the loudest
        assertThat(ringtone[7]).isCloseTo(-0.8912509f, within(1e-6f))
        assertThat(ringtone[0]).isCloseTo(0.1f / 0.25f * 0.8912509f, within(1e-6f))
    }

    @Test
    fun `a silent rendering stays silent, without dividing by zero`() {
        // Given: a loop of 20 s of rests
        val loop = FloatArray(20 * SAMPLE_RATE)

        // When
        val ringtone = ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 10.0)

        // Then
        assertThat(ringtone.toList()).hasSize(loop.size).containsOnly(0f)
    }

    @Test
    fun `an empty rendering is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            ringtoneSamples(floatArrayOf(), SAMPLE_RATE, samplesPerBar = 10.0)
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -7.3, Double.NaN])
    fun `a bar of no length or less is refused`(samplesPerBar: Double) {
        // Given: a loop of 20 s, which is kept once, its bars unused
        val loop = loopOf(seconds = 20)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy {
            ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar)
        }
    }

    @Test
    fun `a bar longer than 60 seconds is refused`() {
        // Given: a loop of 70 s, whose bar lasts a tenth of a sample more than 60 s, so that no whole bar fits
        val loop = loopOf(seconds = 70)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy {
            ringtoneSamples(loop, SAMPLE_RATE, samplesPerBar = 60 * SAMPLE_RATE + 0.1)
        }
    }

    /**
     * Each sample of [actual] is as loud, relative to its loudest, as the one of [expected]: the level aside. Both
     * peak on a positive sample, as [loopOf] does.
     */
    private fun assertSameShape(
        actual: FloatArray,
        expected: FloatArray,
    ) {
        assertThat(actual).hasSameSizeAs(expected)
        val actualPeak = actual.max()
        val expectedPeak = expected.max()
        actual.forEachIndexed { index, sample ->
            assertThat(sample / actualPeak)
                .describedAs("sample $index")
                .isCloseTo(expected[index] / expectedPeak, within(1e-6f))
        }
    }

    /** A loop of [seconds] at [SAMPLE_RATE], whose samples all differ and are positive, peaking at 0.5. */
    private fun loopOf(seconds: Int): FloatArray {
        val size = seconds * SAMPLE_RATE
        return FloatArray(size) { 0.5f * (it + 1) / size }
    }

    private companion object {
        /** Ten samples a second, to keep the renderings small: 20 s are 200 samples. */
        const val SAMPLE_RATE = 10
    }
}
