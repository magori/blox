package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TicksTest {
    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [0, -1])
    fun `a duration of 0 ticks or less is refused`(value: Int) {
        assertThatIllegalArgumentException()
            .isThrownBy { Ticks(value) }
            .withMessageContaining("at least 1")
    }

    @Test
    fun `a duration of 1 tick is the shortest one accepted`() {
        assertThat(Ticks(1).value).isEqualTo(1)
    }

    @Test
    fun `a duration of 1 tick is over after one tick`() {
        assertThat(Ticks(1).afterOneTick()).isNull()
    }

    @Test
    fun `a longer duration has one tick less left after one tick`() {
        assertThat(Ticks(10).afterOneTick()).isEqualTo(Ticks(9))
    }
}
