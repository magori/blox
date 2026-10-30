package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.model.verticalI
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class GameEngineTickTest {
    private val engine = GameEngine(ClassicRules, ScriptedRandomizer())

    @Test
    fun `at level 0 the piece drops one row every 48 ticks, not before`() {
        // Given a T just appeared, pivot on row 0
        val state = gameState(phase = Phase.Falling(tPointingDown(at = Position(5, 0))))

        // When
        val after47 = engine.ticks(state, 47)
        val after48 = engine.ticks(state, 48)
        val after95 = engine.ticks(state, 95)
        val after96 = engine.ticks(state, 96)

        // Then
        assertThat(after47.current?.pivot).isEqualTo(Position(5, 0))
        assertThat(after48.current?.pivot).isEqualTo(Position(5, 1))
        assertThat(after95.current?.pivot).isEqualTo(Position(5, 1))
        assertThat(after96.current?.pivot).isEqualTo(Position(5, 2))
    }

    @Test
    fun `at level 29 the piece drops one row on every tick`() {
        // Given
        val state = gameState(lines = LINES_TO_LEVEL_29, phase = Phase.Falling(tPointingDown(at = Position(5, 0))))

        // When
        val rows = (1..3).map { ticks -> engine.ticks(state, ticks).current?.pivot?.row }

        // Then
        assertThat(rows).containsExactly(1, 2, 3)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("piecesThatCannotDrop")
    fun `when the drop is refused the piece locks, PieceLocked is sent and the entry delay begins`(
        board: Board,
        piece: Piece,
        expectedBoard: Board,
        expectedPhase: Phase,
    ) {
        // Given a piece that cannot drop, at a level where it tries on every tick
        val state = gameState(board = board, lines = LINES_TO_LEVEL_29, phase = Phase.Falling(piece))

        // When
        val update = engine.tick(state)

        // Then
        assertThat(update.state.board).isEqualTo(expectedBoard)
        assertThat(update.events).containsExactly(GameEvent.PieceLocked(piece, bySoftDrop = false))
        assertThat(update.state.phase).isEqualTo(expectedPhase)
    }

    @Test
    fun `at level 0 a piece on the floor locks on the 48th tick, not before`() {
        // Given a T just appeared on the floor, pivot on row 18
        val piece = tPointingDown(at = Position(5, 18))
        val state = gameState(phase = Phase.Falling(piece))

        // When
        val after47 = engine.ticks(state, 47)
        val tick48 = engine.tick(after47)

        // Then
        assertThat(after47.phase).isEqualTo(Phase.Falling(piece, ticksSinceDrop = 47))
        assertThat(after47.board).isEqualTo(Board.empty())
        assertThat(tick48.events).containsExactly(GameEvent.PieceLocked(piece, bySoftDrop = false))
        assertThat(tick48.state.board).isEqualTo(Board.empty().lock(piece))
    }

    @Test
    fun `the next piece appears when the entry delay of the locked piece is over, and a new draw follows it`() {
        // Given a T locking with its pivot on row 18 (entry delay: 10 ticks), and a J to come
        val randomizer = ScriptedRandomizer(PieceType.S)
        val engine = GameEngine(ClassicRules, randomizer)
        val locked = engine.tick(
            gameState(
                next = PieceType.J,
                lines = LINES_TO_LEVEL_29,
                phase = Phase.Falling(tPointingDown(at = Position(5, 18))),
            ),
        ).state

        // When
        val after9 = engine.ticks(locked, 9)
        val after10 = engine.tick(after9).state

        // Then the J appears with its gravity count back to 0
        assertThat(after9.current).isNull()
        assertThat(after10.phase).isEqualTo(Phase.Falling(spawned(PieceType.J), ticksSinceDrop = 0))
        assertThat(after10.next).isEqualTo(PieceType.S)
        assertThat(randomizer.previousArguments).containsExactly(PieceType.J)
    }

    @Test
    fun `a vertical I locked with its pivot on row 18, cells on rows 16 to 19, waits 10 ticks`() {
        // Given
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.S))
        val locked = engine.tick(
            gameState(lines = LINES_TO_LEVEL_29, phase = Phase.Falling(verticalI(at = Position(4, 18)))),
        ).state

        // When
        val ticks = generateSequence(locked) { engine.tick(it).state }.take(20).indexOfFirst { it.current != null }

        // Then
        assertThat(ticks).isEqualTo(10)
    }

    @Test
    fun `if the next piece does not fit when it appears, the game is over and the piece stays shown`() {
        // Given the last tick of an entry delay, a stack that reaches the top in column 5, and a T to come
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.J))
        val state = gameState(
            board = Board.parse(
                """
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                .....O....
                """,
            ),
            next = PieceType.T,
            phase = Phase.EntryDelay(ticksLeft = Ticks(1)),
        )

        // When
        val after = engine.tick(state).state

        // Then
        assertThat(after.phase).isEqualTo(Phase.GameOver(spawned(PieceType.T)))
        assertThat(after.current).isEqualTo(spawned(PieceType.T))
    }

    @Test
    fun `a piece that appears gives no event`() {
        // Given the last tick of an entry delay, on an empty board
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.J))
        val state = gameState(next = PieceType.T, phase = Phase.EntryDelay(ticksLeft = Ticks(1)))

        // When
        val appeared = engine.tick(state)

        // Then
        assertThat(appeared.events).isEmpty()
        assertThat(appeared.state.phase).isEqualTo(Phase.Falling(spawned(PieceType.T)))
    }

    @Test
    fun `a tick after the game is over changes nothing, the tick count included`() {
        // Given a game over on tick 500
        val over = gameState(tickCount = 500, phase = Phase.GameOver(spawned(PieceType.T)))

        // When
        val update = engine.tick(over)

        // Then
        assertThat(update.state).isEqualTo(over)
        assertThat(update.events).isEmpty()
    }

    private companion object {
        @JvmStatic
        fun piecesThatCannotDrop(): List<Arguments> = listOf(
            argumentSet(
                "a T on the floor",
                Board.empty(),
                tPointingDown(at = Position(5, 18)),
                Board.parse(
                    """
                    ....TTT...
                    .....T....
                    """,
                ),
                Phase.EntryDelay(Ticks(10)),
            ),
            argumentSet(
                "a T on the stack",
                Board.parse(
                    """
                    .....O....
                    """,
                ),
                tPointingDown(at = Position(5, 17)),
                Board.parse(
                    """
                    ....TTT...
                    .....T....
                    .....O....
                    """,
                ),
                Phase.EntryDelay(Ticks(12)),
            ),
        )
    }
}
