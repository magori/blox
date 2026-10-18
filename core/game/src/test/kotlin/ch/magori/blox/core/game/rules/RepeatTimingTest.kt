package ch.magori.blox.core.game.rules

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class RepeatTimingTest {
    @Test
    fun `a repeat interval of 0 is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { RepeatTiming(stepsOnPress = true, firstRepeat = 16, interval = 0) }
    }

    @Test
    fun `a first repeat before the press is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { RepeatTiming(stepsOnPress = true, firstRepeat = 0, interval = 6) }
    }
}
