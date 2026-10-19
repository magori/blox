package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.model.verticalI
import ch.magori.blox.core.game.rules.LineClearDelay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GameEngineClearTest {
    private val engine = GameEngine(ClassicRules, ScriptedRandomizer())

    @Test
    fun `a lock that fills rows starts the clear, the board shown still holds the full rows`() {
        // Given a T about to lock with its pivot on row 18, filling rows 18 and 19
        val state =
            gameState(
                board = TWO_ROWS_TO_FILL,
                lines = LINES_TO_LEVEL_29,
                phase = Phase.Falling(tPointingDown(at = Position(5, 18))),
            )

        // When
        val locked = engine.tick(state).state

        // Then
        assertThat(locked.board).isEqualTo(
            Board.parse(
                """
                IIIITTTIII
                IIIIITIIII
                """,
            ),
        )
        assertThat(locked.phase).isInstanceOfSatisfying(Phase.Clearing::class.java) { clearing ->
            assertThat(clearing.rowsCleared.clearedRows).containsExactly(18, 19)
        }
    }

    @ParameterizedTest(name = "lock on tick {0}: {1} ticks")
    @CsvSource("8, 20", "5, 19", "6, 18", "7, 17", "12, 20")
    fun `the clear lasts 16 ticks plus the ticks up to the next multiple of 4 of the game tick count`(
        lockTick: Int,
        expectedTicks: Int,
    ) {
        // Given a T that fills two rows when it locks, on tick number [lockTick] of the game
        val state = gameState(
            board = TWO_ROWS_TO_FILL,
            lines = LINES_TO_LEVEL_29,
            tickCount = lockTick - 1,
            phase = Phase.Falling(tPointingDown(at = Position(5, 18))),
        )

        // When
        val locked = engine.tick(state).state

        // Then
        assertThat(locked.tickCount).isEqualTo(lockTick)
        assertThat(locked.phase).isInstanceOfSatisfying(Phase.Clearing::class.java) { clearing ->
            assertThat(clearing.ticksLeft).isEqualTo(Ticks(expectedTicks))
        }
    }

    @ParameterizedTest(name = "a clear of {0} ticks")
    @CsvSource("1", "3")
    fun `the clear lasts as long as the line clear delay of the rules says`(clearTicks: Int) {
        // Given rules whose clear lasts [clearTicks] ticks, whatever the tick of the lock, and a T that fills two rows
        val fixedDelay = object : LineClearDelay {
            override fun ticksToClear(lockTick: Int): Ticks = Ticks(clearTicks)
        }
        val engine = GameEngine(ClassicRules.copy(lineClearDelay = fixedDelay), ScriptedRandomizer())
        val state = gameState(
            board = TWO_ROWS_TO_FILL,
            lines = LINES_TO_LEVEL_29,
            phase = Phase.Falling(tPointingDown(at = Position(5, 18))),
        )

        // When the piece locks, then [clearTicks] ticks pass
        val locked = engine.tick(state).state
        val updates = generateSequence(engine.tick(locked)) { engine.tick(it.state) }.take(clearTicks).toList()

        // Then the clear took [clearTicks] ticks: on the last one, the rows are cleared, the entry delay begins
        assertThat(locked.phase).isInstanceOfSatisfying(Phase.Clearing::class.java) { clearing ->
            assertThat(clearing.ticksLeft).isEqualTo(Ticks(clearTicks))
        }
        assertThat(updates.dropLast(1)).allSatisfy {
            assertThat(it.state.phase).isInstanceOf(Phase.Clearing::class.java)
        }
        assertThat(updates.last().state.phase).isInstanceOf(Phase.EntryDelay::class.java)
        assertThat(updates.last().events).contains(GameEvent.LinesCleared(LineClear(2)))
    }

    @Test
    fun `when the clear is over the cleared board is shown, lines and level go up, points use the new level`() {
        // Given 9 lines at level 0, and a vertical I that has just cleared 4 rows (20 ticks of clear)
        val locked = engine.tick(FOUR_LINES_ABOUT_TO_LOCK).state

        // When
        val after19 = engine.ticks(locked, 19)
        val tick20 = engine.tick(after19)

        // Then
        assertThat(after19.board).isEqualTo(locked.board)
        assertThat(after19.progress.lines).isEqualTo(Lines(9))
        assertThat(tick20.state.board).isEqualTo(Board.parse("..O......."))
        assertThat(tick20.state.progress.lines).isEqualTo(Lines(13))
        assertThat(tick20.state.progress.level).isEqualTo(Level(1))
        assertThat(tick20.state.progress.score).isEqualTo(Points(2400))
        assertThat(tick20.events).containsExactly(GameEvent.LinesCleared(LineClear(4)))
    }

    @ParameterizedTest(name = "start level {0}, {1} lines, then 2 more -> level {2}")
    @CsvSource("0, 290, 29", "9, 0, 9", "9, 97, 9", "9, 98, 10")
    fun `the level never goes down after a clear`(
        startLevel: Int,
        lines: Int,
        expectedLevel: Int,
    ) {
        // Given a game started at [startLevel] with [lines] lines, and a T about to lock that fills two rows
        val state = gameState(
            board = TWO_ROWS_TO_FILL,
            startLevel = Level(startLevel),
            lines = Lines(lines),
            phase = Phase.Falling(tPointingDown(at = Position(5, 18))),
        )

        // When the piece locks and its clear is over (19 ticks for a lock on tick 1)
        val cleared = generateSequence(engine.tick(state).state) { engine.tick(it).state }
            .take(25)
            .first { it.phase is Phase.EntryDelay }

        // Then the level is the one of the start level and the new total, never lower than before
        assertThat(cleared.progress.lines).isEqualTo(state.progress.lines + LineClear(2))
        assertThat(cleared.progress.startLevel).isEqualTo(state.progress.startLevel)
        assertThat(cleared.progress.level).isEqualTo(Level(expectedLevel))
        assertThat(cleared.progress.level.value).isGreaterThanOrEqualTo(state.progress.level.value)
    }

    @Test
    fun `after the clear comes the entry delay computed when the piece locked, then the next piece`() {
        // Given a vertical I that locked with its pivot on row 18 (entry delay: 10 ticks), its clear over
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.S))
        val cleared = engine.ticks(engine.tick(FOUR_LINES_ABOUT_TO_LOCK).state, 20)

        // When
        val after9 = engine.ticks(cleared, 9)
        val after10 = engine.tick(after9).state

        // Then
        assertThat(cleared.phase).isEqualTo(Phase.EntryDelay(Ticks(10)))
        assertThat(after9.current).isNull()
        assertThat(after10.current).isEqualTo(spawned(PieceType.J))
    }

    @Test
    fun `a four-line clear counts in the statistics of the game`() {
        // Given a vertical I that has just cleared 4 rows
        val locked = engine.tick(FOUR_LINES_ABOUT_TO_LOCK).state

        // When the clear is over
        val cleared = engine.ticks(locked, 20)

        // Then
        assertThat(locked.progress.statistics.fourLineClears).isEqualTo(0)
        assertThat(cleared.progress.statistics.fourLineClears).isEqualTo(1)
    }

    @Test
    fun `the ticks between the lock and the end of the clear give no event`() {
        // Given a vertical I that has just cleared 4 rows (20 ticks of clear)
        val locked = engine.tick(FOUR_LINES_ABOUT_TO_LOCK).state

        // When
        val events = generateSequence(engine.tick(locked)) { engine.tick(it.state) }.take(19).map { it.events }.toList()

        // Then
        assertThat(events).hasSize(19).allSatisfy { assertThat(it).isEmpty() }
    }

    private companion object {
        /**
         * 9 lines at level 0, a vertical I about to lock on tick 4 with its pivot at (0, 18), filling rows
         * 16 to 19; a J to come.
         */
        val FOUR_LINES_ABOUT_TO_LOCK = gameState(
            board = Board.parse(
                """
                ..O.......
                .IIIIIIIII
                .IIIIIIIII
                .IIIIIIIII
                .IIIIIIIII
                """,
            ),
            next = PieceType.J,
            lines = Lines(9),
            tickCount = 3,
            phase = Phase.Falling(verticalI(at = Position(0, 18)), ticksSinceDrop = 47),
        )
    }
}
