package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.model.verticalI
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class GameEngineGameOverTest {
    private val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.J))

    @Test
    fun `GameOver is sent once, on the tick where the next piece cannot appear`() {
        // Given the last tick of an entry delay, and a stack where a T cannot appear
        val state = gameState(board = TOWER, next = PieceType.T, phase = Phase.EntryDelay(ticksLeft = 1))

        // When
        val over = engine.tick(state)
        val after = engine.tick(over.state)

        // Then
        assertThat(over.state.phase).isEqualTo(Phase.GameOver(spawned(PieceType.T)))
        assertThat(over.events).containsExactly(GameEvent.GameOver)
        assertThat(after.events).isEmpty()
    }

    /**
     * The "never off" holds by the thresholds of [ch.magori.blox.core.game.rules.ClassicStackDangerRule]: a
     * blocked appearance leaves a stack at least 19 rows high, and the rule only leaves the danger at 12 or less.
     */
    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("lastPieces")
    fun `the last events are PieceLocked, then LinesCleared if any, then GameOver, and never StackDanger(off)`(
        board: Board,
        lastPiece: Piece,
        wasInDanger: Boolean,
        expectedEvents: List<GameEvent>,
    ) {
        // Given the last piece about to lock; a T comes next, and cannot appear
        val state = gameState(
            board = board,
            next = PieceType.T,
            lines = LINES_TO_LEVEL_29,
            stackInDanger = wasInDanger,
            phase = Phase.Falling(lastPiece),
        )

        // When the game goes on to its end
        val updates = generateSequence(engine.tick(state)) { engine.tick(it.state) }
            .take(60)
            .takeWhile { it.state.phase !is Phase.GameOver || GameEvent.GameOver in it.events }
            .toList()

        // Then
        assertThat(updates.last().state.phase).isInstanceOf(Phase.GameOver::class.java)
        assertThat(updates.flatMap { it.events }).containsExactlyElementsOf(expectedEvents)
    }

    private companion object {
        /** A column of 20 cells in column 5: no piece can appear. */
        val TOWER = Board.parse(List(20) { ".....O...." }.joinToString("\n"))

        /** Column 5 full from row 0 to row 18, and row 19 full but for column 0. */
        val TOWER_ON_A_ROW_TO_CLEAR = Board.parse((List(19) { ".....O...." } + ".IIIIIIIII").joinToString("\n"))

        /** A T on the floor, against the left wall. */
        val T_ON_THE_FLOOR = tPointingDown(at = Position(1, 18))

        /** A vertical I in column 0 that fills row 19. */
        val I_FILLING_ROW_19 = verticalI(at = Position(0, 18))

        /** Column 5 full from row 2 to row 19: 18 rows high, out of danger until a piece locks on top of it. */
        val TOWER_BELOW_THE_TOP = Board.parse(List(18) { ".....O...." }.joinToString("\n"))

        /** A vertical I that locks on [TOWER_BELOW_THE_TOP], up to the top of the board. */
        val I_ON_TOP_OF_THE_TOWER = verticalI(at = Position(5, 0))

        @JvmStatic
        fun lastPieces(): List<Arguments> = listOf(
            argumentSet(
                "a lock without a line clear, the stack in danger",
                TOWER,
                T_ON_THE_FLOOR,
                true,
                listOf(GameEvent.PieceLocked(T_ON_THE_FLOOR), GameEvent.GameOver),
            ),
            argumentSet(
                "a lock that raises the stack to the top: the danger starts, then the game ends",
                TOWER_BELOW_THE_TOP,
                I_ON_TOP_OF_THE_TOWER,
                false,
                listOf(
                    GameEvent.PieceLocked(I_ON_TOP_OF_THE_TOWER),
                    GameEvent.StackDanger(on = true),
                    GameEvent.GameOver,
                ),
            ),
            argumentSet(
                "a lock that clears a line, the stack in danger",
                TOWER_ON_A_ROW_TO_CLEAR,
                I_FILLING_ROW_19,
                true,
                listOf(
                    GameEvent.PieceLocked(I_FILLING_ROW_19),
                    GameEvent.LinesCleared(LineClear(1)),
                    GameEvent.GameOver,
                ),
            ),
        )
    }
}
