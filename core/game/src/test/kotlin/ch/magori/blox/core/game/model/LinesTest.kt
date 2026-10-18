package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class LinesTest {
    @Test
    fun `a total of lines cannot be negative`() {
        assertThatIllegalArgumentException().isThrownBy { Lines(-1) }
    }

    @Test
    fun `a total of 0 lines is the lowest valid total`() {
        assertThat(Lines(0).value).isEqualTo(0)
    }

    @Test
    fun `the lines cleared at once add to the total`() {
        assertThat(Lines(9) + LineClear(4)).isEqualTo(Lines(13))
    }
}
