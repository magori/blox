package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Ticks
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ClassicLineClearDelayTest {
    private val lineClearDelay: LineClearDelay = ClassicLineClearDelay

    @ParameterizedTest(name = "lock on tick {0}: {1} ticks")
    @CsvSource("8, 20", "5, 19", "6, 18", "7, 17", "12, 20", "1, 19", "4, 20")
    fun `the clear lasts 16 ticks plus the ticks up to the next multiple of 4 of the lock tick`(
        lockTick: Int,
        expectedTicks: Int,
    ) {
        // When
        val ticks = lineClearDelay.ticksToClear(lockTick)

        // Then 5 passes, one every 4 ticks: the last one on a multiple of 4
        assertThat(ticks).isEqualTo(Ticks(expectedTicks))
        assertThat((lockTick + ticks.value) % 4).isZero()
    }

    @ParameterizedTest(name = "lock on tick {0}")
    @ValueSource(ints = [-1, -4, Int.MIN_VALUE])
    fun `a negative lock tick is refused`(lockTick: Int) {
        // When / Then: the tick count of a game is never negative
        assertThatIllegalArgumentException().isThrownBy { lineClearDelay.ticksToClear(lockTick) }
    }
}
