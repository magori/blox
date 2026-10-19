package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class PhaseTest {
    @Test
    fun `a falling piece cannot have a negative gravity count`() {
        assertThatIllegalArgumentException()
            .isThrownBy { Phase.Falling(tPointingDown(at = Position(5, 0)), ticksSinceDrop = -1) }
            .withMessageContaining("ticksSinceDrop")
    }

    @Test
    fun `an entry delay lasts at least one tick`() {
        assertThatIllegalArgumentException()
            .isThrownBy { Phase.EntryDelay(ticksLeft = Ticks(0)) }
            .withMessageContaining("at least 1 tick")
    }

    @Test
    fun `a clear lasts at least one tick`() {
        val rowsCleared = Board.parse("IIIIIIIIII").clearFullRows()
        assertThatIllegalArgumentException()
            .isThrownBy {
                Phase.Clearing(
                    rowsCleared,
                    ticksLeft = Ticks(0),
                    entryDelayAfter = Ticks(10),
                    stackInDangerAfter = false,
                )
            }
            .withMessageContaining("at least 1 tick")
    }
}
