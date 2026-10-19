package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class QualityTest {
    @Test
    fun `a candidate meeting every criterion in full scores 100, the weights of the criteria adding up to it`() {
        // Given
        val quality = Quality(Criterion.entries.associateWith { 1.0 })

        // When
        val points = quality.points

        // Then
        assertThat(points).isEqualTo(100.0)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Criterion::class)
    internal fun `the points grow by the weight of a criterion as far as it is better met, the others staying`(
        criterion: Criterion,
    ) {
        // Given
        val half = Criterion.entries.associateWith { 0.5 }
        val better = half + (criterion to 0.8)

        // When
        val gain = Quality(better).points - Quality(half).points

        // Then
        assertThat(gain).isCloseTo(criterion.weight * 0.3, within(1e-9))
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(doubles = [-0.1, 1.1, Double.NaN])
    fun `a criterion met less than not at all, more than in full, or by a share that is not a number, is refused`(
        fulfilment: Double,
    ) {
        // Given
        val fulfilments = Criterion.entries.associateWith { 1.0 } + (Criterion.STEPS to fulfilment)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Quality(fulfilments) }
    }

    @Test
    fun `a quality that leaves a criterion out is refused`() {
        // Given
        val fulfilments = Criterion.entries.associateWith { 1.0 } - Criterion.ARCH

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Quality(fulfilments) }
    }

    @ParameterizedTest(name = "{0} of steps for {1}: {2}")
    @CsvSource(
        "0.7, 0.7, 1",
        "0.85, 0.7, 0.5",
        "0.55, 0.7, 0.5",
        "0.4, 0.7, 0",
        "1, 0.7, 0",
        "0.6, 0.6, 1",
        "0.75, 0.6, 0.5",
    )
    fun `the steps are met in full at the share the category aims at, and not at all 30 points away or more`(
        share: Double,
        ideal: Double,
        expected: Double,
    ) {
        // When
        val fulfilment = stepsFulfilment(share, ideal)

        // Then
        assertThat(fulfilment).isCloseTo(expected, within(1e-9))
    }

    @ParameterizedTest(name = "{0} semitones: {1}")
    @CsvSource("12, 1", "14, 1", "16, 1", "17, 0.8", "10, 0.6", "7, 0", "21, 0", "0, 0")
    fun `the span is met in full from an octave to a major tenth, and not at all a fourth further either way`(
        semitones: Int,
        expected: Double,
    ) {
        // When
        val fulfilment = spanFulfilment(semitones)

        // Then
        assertThat(fulfilment).isCloseTo(expected, within(1e-9))
    }

    @Test
    fun `the arch is met by a highest note struck once, in bar 5 or 6, and not at all otherwise`() {
        // Given the bars, counted from 0, where the highest note is struck: once in bar 5, once in bar 6, in both, once
        // in bar 4, once in bar 7, and never
        val peaks = listOf(listOf(4), listOf(5), listOf(4, 5), listOf(3), listOf(6), emptyList())

        // When
        val fulfilments = peaks.map(::archFulfilment)

        // Then
        assertThat(fulfilments).containsExactly(1.0, 1.0, 0.0, 0.0, 0.0, 0.0)
    }

    @Test
    fun `the quality of a candidate reads each criterion off the measures of its melody and its bass`() {
        // Given, in joyful, which aims at 69 % of steps and leans no way, a melody going up by thirds and steps, 4 steps
        // out of 9 moves, from C4 to F5, 17 semitones, through D5, out of the chord and left by a leap, its only
        // unprepared note, with half of the moves of the motif kept, and its peak, F5, once in bar 5; a bass leaping a
        // fifth from C to F, then making its four other changes of chord, loop included, by a fourth at most
        val form = formIn(C_MAJOR, "C", "F", "G", "C", "F")
        val melody = melodyOf("C4 2, E4 2", "F4 2, A4 2", "G4 2, B4 2", "C5 2, D5 2", "F5 2, C5 2")
        val bass = bassOf("C3 4", "F2 4", "G2 4", "C3 4", "F3 4")

        // When
        val quality = Quality.of(form, melody, bass)

        // Then
        val expected = mapOf(
            Criterion.STEPS to 49.0 / 270,
            Criterion.PREPARED_NOTES to 15.0 / 16,
            Criterion.MOTIF to 0.5,
            Criterion.SMOOTH_BASS to 0.8,
            Criterion.LEANING to 1.0,
            Criterion.SPAN to 0.8,
            Criterion.ARCH to 1.0,
        )
        assertThat(quality.fulfilment).hasSameSizeAs(expected).allSatisfy { criterion, fulfilment ->
            assertThat(fulfilment).describedAs("%s", criterion).isCloseTo(expected.getValue(criterion), within(1e-9))
        }
    }

    @Test
    fun `under the same melody, a bass leaping less from chord to chord scores higher, by that criterion alone`() {
        // Given the bass that leaps a fifth from C to F, and one that goes up a fourth instead, then on by a third at most
        val form = formIn(C_MAJOR, "C", "F", "G", "C", "F")
        val melody = melodyOf("C4 2, E4 2", "F4 2, A4 2", "G4 2, B4 2", "C5 2, D5 2", "F5 2, C5 2")
        val leaping = Quality.of(form, melody, bassOf("C3 4", "F2 4", "G2 4", "C3 4", "F3 4"))

        // When
        val smooth = Quality.of(form, melody, bassOf("C3 4", "F3 4", "G3 4", "E3 4", "F3 4"))

        // Then
        assertThat(smooth.points).isGreaterThan(leaping.points)
        assertThat(smooth.fulfilment - Criterion.SMOOTH_BASS).isEqualTo(leaping.fulfilment - Criterion.SMOOTH_BASS)
    }

    @ParameterizedTest(name = "{0} unprepared: {1}")
    @CsvSource("0, 1", "1, 0.9375", "4, 0.75", "8, 0.5", "16, 0", "20, 0")
    fun `each unprepared note costs a sixteenth of the prepared notes, down to not at all from sixteen of them`(
        unprepared: Int,
        expected: Double,
    ) {
        // When
        val fulfilment = preparedFulfilment(unprepared)

        // Then
        assertThat(fulfilment).isCloseTo(expected, within(1e-9))
    }

    @ParameterizedTest(name = "{0} of the moves for {1}: {2}")
    @CsvSource("0, 0.1, 0", "0.05, 0.1, 0.5", "0.1, 0.1, 1", "0.2, 0.1, 1", "0.33, 0.33, 1")
    fun `a melody leans as its category does in full from the share of moves it aims at, and in part below it`(
        share: Double,
        target: Double,
        expected: Double,
    ) {
        // When
        val fulfilment = leaningFulfilment(share, target)

        // Then
        assertThat(fulfilment).isCloseTo(expected, within(1e-9))
    }

    @Test
    fun `a melody of retro leans as far as its share of arpeggios goes towards the third of its moves retro aims at`() {
        // Given, under C major, C E between two notes of the chord, then E F, F D and D C: one move out of four
        val form = formIn(C_MAJOR, "C", "C").copy(category = Category.RETRO)
        val melody = melodyOf("C4 1, E4 1, F4 1, D4 1", "C4 4")

        // When
        val quality = Quality.of(form, melody, bassOf("C3 4", "C3 4"))

        // Then
        assertThat(quality.fulfilment.getValue(Criterion.LEANING)).isCloseTo(0.25 / 0.33, within(1e-9))
    }
}
