package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class PointsTest {
    @Test
    fun `points cannot be negative`() {
        assertThatIllegalArgumentException()
            .isThrownBy { Points(-1) }
            .withMessageContaining("negative")
    }

    @Test
    fun `points add up`() {
        assertThat(Points(1200) + Points(40)).isEqualTo(Points(1240))
    }
}
