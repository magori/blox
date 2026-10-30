package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class AccompanimentTest {
    @ParameterizedTest(name = "{0} ticks per eighth")
    @CsvSource("18, 1.0", "16, 1.125", "15, 1.2", "12, 1.5", "10, 1.8")
    fun `the tempo factor is 18 over the ticks per eighth`(
        ticksPerEighth: Int,
        expected: Double,
    ) {
        // When
        val factor = Accompaniment.tempoFactor(ticksPerEighth)

        // Then: written at 100 beats per minute, the accompaniment plays at 1 800 over the ticks per eighth
        assertThat(factor).isCloseTo(expected, within(1e-9))
        assertThat(Accompaniment.score(metronome = true).bpm * factor)
            .isCloseTo(1800.0 / ticksPerEighth, within(1e-9))
    }

    @Test
    fun `the bass plays I V vi IV, one bar each`() {
        // Given
        val score = Accompaniment.score(metronome = true)

        // When
        val bass = score.bass()

        // Then: C, G, Am and F of C major, the root of each chord on every eighth of its bar, at the volume of a bass
        assertThat(bass).isEqualTo(bassVoice(bass.notes))
        assertThat(barsOf(bass, score.beatsPerBar).map { bar -> bar.joinToString { "${it.pitch} ${it.beats}" } })
            .containsExactly(
                List(8) { "C3 0.5" }.joinToString(),
                List(8) { "G2 0.5" }.joinToString(),
                List(8) { "A2 0.5" }.joinToString(),
                List(8) { "F2 0.5" }.joinToString(),
            )
    }

    @Test
    fun `the accompaniment clicks on every quarter, louder on beat 1`() {
        // Given
        val score = Accompaniment.score(metronome = true)

        // When
        val clicks = score.voices.filter { it.waveform == Waveform.SQUARE }
        val onBeat1 = clicks.single { voice -> struckIn(barsOf(voice, score.beatsPerBar)).all { it.onset == 0.0 } }
        val onOtherBeats = clicks.single { it != onBeat1 }

        // Then: a high C on beat 1 of every bar, a G below it on beats 2 to 4, each shorter than an eighth
        assertThat(struckIn(barsOf(onBeat1, score.beatsPerBar)))
            .containsExactlyElementsOf((0..3).map { bar -> StruckNote(bar, 0.0, Pitch.parse("C6")) })
        assertThat(struckIn(barsOf(onOtherBeats, score.beatsPerBar))).containsExactlyElementsOf(
            (0..3).flatMap { bar -> listOf(1.0, 2.0, 3.0).map { StruckNote(bar, it, Pitch.parse("G5")) } },
        )
        assertThat(onBeat1.volume).isGreaterThan(onOtherBeats.volume)
        assertThat(clicks.flatMap { it.notes }.filterNot { it.isRest }.map { it.beats }).allMatch { it < 0.5 }
    }

    @Test
    fun `without the metronome the bass plays on`() {
        // Given
        val withMetronome = Accompaniment.score(metronome = true)

        // When
        val withoutMetronome = Accompaniment.score(metronome = false)

        // Then: the same bass alone, at the same tempo, so that the music goes on where it was
        assertThat(withoutMetronome.voices).containsExactly(withMetronome.bass())
        assertThat(withoutMetronome.copy(voices = withMetronome.voices)).isEqualTo(withMetronome)
    }

    @ParameterizedTest(name = "{0} ticks per eighth")
    @ValueSource(ints = [0, -15])
    fun `an eighth lasts a positive number of ticks`(ticksPerEighth: Int) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Accompaniment.tempoFactor(ticksPerEighth) }
    }

    @ParameterizedTest(name = "metronome {0}")
    @ValueSource(booleans = [true, false])
    fun `every voice of the accompaniment starts a note or a rest on every beat`(metronome: Boolean) {
        // Given
        val score = Accompaniment.score(metronome)

        // When
        val starts = score.voices.map { voice -> voice.notes.runningFold(0.0) { start, note -> start + note.beats } }

        // Then: the music may switch or resume on any beat, no note nor rest crossing it
        assertThat(starts).allSatisfy { assertThat(it).containsAll((0 until 16).map(Int::toDouble)) }
    }

    /** The bass of [this] accompaniment, its triangle voice. */
    private fun Score.bass(): Voice = voices.single { it.waveform == Waveform.TRIANGLE }
}
