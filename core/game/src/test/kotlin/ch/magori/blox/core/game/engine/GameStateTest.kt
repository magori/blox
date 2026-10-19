package ch.magori.blox.core.game.engine

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class GameStateTest {
    @Test
    fun `a negative tick count is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { gameState(tickCount = -1) }
            .withMessage("A tick count cannot be negative: -1")
    }
}
