package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.model.verticalI
import ch.magori.blox.core.game.rules.ClassicRotationSystem
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
        val state = gameState(board = TOWER, next = PieceType.T, phase = Phase.EntryDelay(ticksLeft = Ticks(1)))

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
        val updates = engine.updatesToTheEnd(state)

        // Then
        assertThat(updates.last().state.phase).isInstanceOf(Phase.GameOver::class.java)
        assertThat(updates.flatMap { it.events }).containsExactlyElementsOf(expectedEvents)
    }

    @Test
    fun `a last line clear that reaches the next level sends PieceLocked, LinesCleared, LevelUp, then GameOver`() {
        // Given 289 lines at level 28, the stack in danger, and the last piece about to clear one line: 290 lines,
        // level 29; a T comes next, and cannot appear
        val state = gameState(
            board = TOWER_ON_A_ROW_TO_CLEAR,
            next = PieceType.T,
            lines = Lines(289),
            stackInDanger = true,
            phase = Phase.Falling(I_FILLING_ROW_19),
        )

        // When the game goes on to its end
        val updates = engine.updatesToTheEnd(state)

        // Then
        assertThat(updates.last().state.phase).isInstanceOf(Phase.GameOver::class.java)
        assertThat(updates.flatMap { it.events }).containsExactly(
            GameEvent.PieceLocked(I_FILLING_ROW_19, bySoftDrop = false),
            GameEvent.LinesCleared(LineClear(1)),
            GameEvent.LevelUp(Level(29)),
            GameEvent.GameOver,
        )
    }

    @Test
    fun `a piece that turns then locks for the last time leaves GameOver alone on the tick it ends`() {
        // Given a T on the floor beside a column where no piece can appear, A pressed: it turns, then locks
        val state = engine.press(
            gameState(
                board = TOWER,
                next = PieceType.T,
                lines = LINES_TO_LEVEL_29,
                stackInDanger = true,
                phase = Phase.Falling(T_ON_THE_FLOOR),
            ),
            Button.A,
        )

        // When the game goes on to its end
        val updates = engine.updatesToTheEnd(state)

        // Then
        val turned = ClassicRotationSystem.candidates(T_ON_THE_FLOOR, Rotation.CLOCKWISE).single()
        assertThat(updates.first().events).containsExactly(
            GameEvent.PieceRotated(turned, Rotation.CLOCKWISE),
            GameEvent.PieceLocked(turned, bySoftDrop = false),
        )
        assertThat(updates.last().events).containsExactly(GameEvent.GameOver)
        assertThat(updates.drop(1).dropLast(1).flatMap { it.events }).isEmpty()
    }

    /** The updates of the ticks from [state] up to the one that ends the game, within a second. */
    private fun GameEngine.updatesToTheEnd(state: GameState): List<GameUpdate> =
        generateSequence(tick(state)) { tick(it.state) }
            .take(60)
            .takeWhile { it.state.phase !is Phase.GameOver || GameEvent.GameOver in it.events }
            .toList()

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
                listOf(GameEvent.PieceLocked(T_ON_THE_FLOOR, bySoftDrop = false), GameEvent.GameOver),
            ),
            argumentSet(
                "a lock that raises the stack to the top: the danger starts, then the game ends",
                TOWER_BELOW_THE_TOP,
                I_ON_TOP_OF_THE_TOWER,
                false,
                listOf(
                    GameEvent.PieceLocked(I_ON_TOP_OF_THE_TOWER, bySoftDrop = false),
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
                    GameEvent.PieceLocked(I_FILLING_ROW_19, bySoftDrop = false),
                    GameEvent.LinesCleared(LineClear(1)),
                    GameEvent.GameOver,
                ),
            ),
        )
    }
}
