package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class OrientationTest {
    private val a = Position(-1, 0)
    private val b = Position(0, 0)
    private val c = Position(1, 0)
    private val d = Position(0, 1)
    private val e = Position(0, 2)

    @Test
    fun `an orientation needs exactly four distinct cells`() {
        assertThat(Orientation(setOf(a, b, c, d)).cells).containsExactlyInAnyOrder(a, b, c, d)
        assertThatIllegalArgumentException()
            .isThrownBy { Orientation(setOf(a, b, c)) }
            .withMessageContaining("4 cells")
        assertThatIllegalArgumentException()
            .isThrownBy { Orientation(setOf(a, b, c, d, e)) }
            .withMessageContaining("4 cells")
        assertThatIllegalArgumentException()
            .isThrownBy { Orientation(setOf(a, b, c, c)) }
            .withMessageContaining("4 cells")
    }
}
