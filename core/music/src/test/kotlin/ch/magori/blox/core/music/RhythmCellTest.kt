package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class RhythmCellTest {
    @Test
    fun `a cell is read as durations in beats separated by spaces, r marking a rest`() {
        // When
        val cell = RhythmCell.parse("1.5 0.5 1 r1")

        // Then
        assertThat(cell.steps).containsExactly(
            RhythmStep(1.5),
            RhythmStep(0.5),
            RhythmStep(1.0),
            RhythmStep(1.0, isRest = true),
        )
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["1 1 1", "2 2 0.5", "1 1 1 1 r1"])
    fun `a cell that does not fill exactly one bar of four beats is refused`(steps: String) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { RhythmCell.parse(steps) }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["0 4", "-1 5", "r-1 5", "x 4"])
    fun `a step that is not a positive number of beats is refused, even if the bar adds up`(steps: String) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { RhythmCell.parse(steps) }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["r1 3", "r0.5 3.5", "2 r2", "1 r3", "1.5 r1 1.5"])
    fun `a cell that is silent on beat 1 or 3 rests on a strong beat`(steps: String) {
        // When
        val cell = RhythmCell.parse(steps)

        // Then
        assertThat(cell.restsOnStrongBeat).isTrue()
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["1 r1 1.5 r0.5", "4"])
    fun `a cell silent only on the weak beats does not rest on a strong beat`(steps: String) {
        // When
        val cell = RhythmCell.parse(steps)

        // Then
        assertThat(cell.restsOnStrongBeat).isFalse()
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        "0.5 1 1 1 0.5, 3",
        "0.5 1 0.5 0.5 1 0.5, 2",
        "0.5 1 0.5 2, 1",
        "0.5 1 0.5 0.5 r1 0.5, 1",
        "0.75 0.25 0.75 0.25 2, 0",
        "1 2 1, 1",
        "1 1.5 1.5, 2",
        "2 2, 0",
        "1 1 2, 0",
    )
    fun `a cell counts its syncopations, notes struck off the strong beats and held over the next beat`(
        steps: String,
        syncopations: Int,
    ) {
        // When
        val cell = RhythmCell.parse(steps)

        // Then
        assertThat(cell.syncopations).isEqualTo(syncopations)
    }

    @Test
    fun `a phrase may end by resting from beat 3, to breathe`() {
        // Given / When / Then
        assertThatCode { RhythmCell.parse("2 r2") }.doesNotThrowAnyException()
    }
}
