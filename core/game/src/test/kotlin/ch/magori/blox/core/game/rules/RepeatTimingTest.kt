package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Ticks
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class RepeatTimingTest {
    @Test
    fun `a repeat interval of 0 is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { RepeatTiming(stepsOnPress = true, firstRepeat = Ticks(16), interval = Ticks(0)) }
    }

    @Test
    fun `a first repeat before the press is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { RepeatTiming(stepsOnPress = true, firstRepeat = Ticks(0), interval = Ticks(6)) }
    }

    @Test
    fun `without a step on the press, no step is due on the tick of the press`() {
        // Given a timing that does not step on the press
        val timing = RepeatTiming(stepsOnPress = false, firstRepeat = Ticks(2), interval = Ticks(2))

        // When / Then
        assertThat(timing.stepsOn(tick = 0)).isFalse()
    }

    @Test
    fun `a first repeat and an interval of 1 tick give a step on every tick after the press`() {
        // Given the shortest timing, without a step on the press
        val timing = RepeatTiming(stepsOnPress = false, firstRepeat = Ticks(1), interval = Ticks(1))

        // When / Then
        assertThat((0..5).filter(timing::stepsOn)).containsExactly(1, 2, 3, 4, 5)
    }

    @Test
    fun `no step is due on the tick just before the first repeat`() {
        // Given a first repeat 16 ticks after the press
        val timing = RepeatTiming(stepsOnPress = true, firstRepeat = Ticks(16), interval = Ticks(1))

        // When / Then
        assertThat(timing.stepsOn(tick = 15)).isFalse()
        assertThat(timing.stepsOn(tick = 16)).isTrue()
    }
}
