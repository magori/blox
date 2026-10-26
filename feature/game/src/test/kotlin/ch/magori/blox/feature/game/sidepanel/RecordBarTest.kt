package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.game.model.Points
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class RecordBarTest {
    @ParameterizedTest(name = "{0} points of a record of {1}: {2}")
    @CsvSource("0, 1000, 0", "250, 1000, 0.25", "999, 1000, 0.999", "1000, 1000, 1", "1001, 1000, 1", "5000000, 1, 1")
    fun `the record bar fills with the score up to the record`(
        score: Int,
        record: Int,
        fill: Float,
    ) {
        // When
        val bar = recordBarFill(Points(score), record)

        // Then full once the score reaches the record, never beyond
        assertThat(bar).isEqualTo(fill)
    }

    @ParameterizedTest(name = "a record of {0}")
    @NullSource
    @ValueSource(ints = [0])
    fun `without a record the record bar is hidden`(record: Int?) {
        // When
        val bar = recordBarFill(Points(1200), record)

        // Then
        assertThat(bar).isNull()
    }
}
