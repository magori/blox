package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.model.verticalI
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GameEngineStackDangerTest {
    private val engine = GameEngine(ClassicRules, ScriptedRandomizer())

    @ParameterizedTest(name = "in danger before: {0} -> in danger: {1}")
    @CsvSource("false, true", "true, true")
    fun `without a clear, the danger is measured when the piece locks and StackDanger is sent only when it changes`(
        wasInDanger: Boolean,
        expectedInDanger: Boolean,
    ) {
        // Given a T about to lock at height 15
        val state = gameState(
            board = TOWER,
            lines = LINES_TO_LEVEL_29,
            stackInDanger = wasInDanger,
            phase = Phase.Falling(T_ON_TOWER),
        )

        // When
        val update = engine.tick(state)

        // Then
        assertThat(update.state.stackInDanger).isEqualTo(expectedInDanger)
        val expectedEvents = buildList {
            add(GameEvent.PieceLocked(T_ON_TOWER))
            if (expectedInDanger != wasInDanger) add(GameEvent.StackDanger(on = expectedInDanger))
        }
        assertThat(update.events).containsExactlyElementsOf(expectedEvents)
    }

    @Test
    fun `four lines locked at height 15 leave the stack 11 rows high, with no danger and no StackDanger`() {
        // Given
        val state = gameState(
            board = FOUR_LINES_UNDER_A_TOWER,
            lines = LINES_TO_LEVEL_29,
            phase = Phase.Falling(I_FOR_FOUR_LINES),
        )

        // When
        val lock = engine.tick(state)
        val clearEnd = engine.tick(engine.ticks(lock.state, CLEAR_TICKS - 1))

        // Then
        assertThat(lock.events).containsExactly(GameEvent.PieceLocked(I_FOR_FOUR_LINES))
        assertThat(clearEnd.events).containsExactly(GameEvent.LinesCleared(LineClear(4)))
        assertThat(clearEnd.state.stackInDanger).isFalse()
    }

    @Test
    fun `with a clear, StackDanger comes at the end of the animation, with LinesCleared, as the stack goes down`() {
        // Given a stack in danger, and four lines that bring it down to 11 rows
        val state = gameState(
            board = FOUR_LINES_UNDER_A_TOWER,
            lines = LINES_TO_LEVEL_29,
            stackInDanger = true,
            phase = Phase.Falling(I_FOR_FOUR_LINES),
        )

        // When
        val lock = engine.tick(state)
        val clearEnd = engine.tick(engine.ticks(lock.state, CLEAR_TICKS - 1))

        // Then
        assertThat(lock.events).containsExactly(GameEvent.PieceLocked(I_FOR_FOUR_LINES))
        assertThat(lock.state.stackInDanger).isTrue()
        assertThat(clearEnd.events).containsExactly(
            GameEvent.LinesCleared(LineClear(4)),
            GameEvent.StackDanger(on = false),
        )
        assertThat(clearEnd.state.stackInDanger).isFalse()
    }

    @Test
    fun `a piece that turns then locks into the danger sends PieceRotated, then PieceLocked, then StackDanger`() {
        // Given a T about to lock at height 15, A pressed: turned, it locks at height 16 on the same tick
        val state = engine.press(
            gameState(board = TOWER, lines = LINES_TO_LEVEL_29, phase = Phase.Falling(T_ON_TOWER)),
            Button.A,
        )

        // When
        val update = engine.tick(state)

        // Then
        val turned = ClassicRotationSystem.candidates(T_ON_TOWER, Rotation.CLOCKWISE).single()
        assertThat(update.events).containsExactly(
            GameEvent.PieceRotated(turned),
            GameEvent.PieceLocked(turned),
            GameEvent.StackDanger(on = true),
        )
    }

    private companion object {
        /** A column 13 rows high: a T pointing down with its pivot at (5, 5) locks on it, at height 15. */
        val TOWER = Board.parse(List(13) { ".....O...." }.joinToString("\n"))

        /** A T that locks on [TOWER], at height 15. */
        val T_ON_TOWER = tPointingDown(at = Position(5, 5))

        /**
         * Rows 16 to 19 full but for column 0, under a column that reaches row 5: a vertical I with its pivot
         * at (0, 18) locks at height 15 and clears 4 rows, which leaves the stack 11 rows high.
         */
        val FOUR_LINES_UNDER_A_TOWER = Board.parse(
            (List(11) { ".........O" } + List(4) { ".IIIIIIIII" }).joinToString("\n"),
        )

        /** The I that clears 4 rows on [FOUR_LINES_UNDER_A_TOWER]. */
        val I_FOR_FOUR_LINES = verticalI(at = Position(0, 18))

        /** The clear animation of a lock on tick 1 of the game: 16 ticks, plus 3 up to tick 4. */
        const val CLEAR_TICKS = 19
    }
}
