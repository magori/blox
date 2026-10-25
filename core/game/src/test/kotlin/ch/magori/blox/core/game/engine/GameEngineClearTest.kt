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
    }

    @ParameterizedTest(name = "start level {0}, {1} lines, then 2 more -> level {2}")
    @CsvSource("0, 8, 1", "0, 18, 2", "5, 58, 6", "9, 98, 10", "0, 288, 29")
    fun `a line clear that reaches the next level sends a level up with the new level`(
        startLevel: Int,
        lines: Int,
        expectedLevel: Int,
    ) {
        // Given a game started at [startLevel] with [lines] lines, and a T about to lock that fills two rows
        val state = twoRowsAboutToLock(Level(startLevel), Lines(lines))

        // When the piece locks and its clear is over
        val clearEnd = engine.endOfClear(state)

        // Then
        assertThat(clearEnd.events).containsExactly(
            GameEvent.LinesCleared(LineClear(2)),
            GameEvent.LevelUp(Level(expectedLevel)),
        )
    }

    @ParameterizedTest(name = "start level {0}, {1} lines, then 2 more")
    @CsvSource("0, 0", "0, 7", "0, 290", "5, 50")
    fun `a line clear that stays at the same level sends no level up`(
        startLevel: Int,
        lines: Int,
    ) {
        // Given a game started at [startLevel] with [lines] lines, and a T about to lock that fills two rows
        val state = twoRowsAboutToLock(Level(startLevel), Lines(lines))

        // When the piece locks and its clear is over
        val clearEnd = engine.endOfClear(state)

        // Then
        assertThat(clearEnd.state.progress.level).isEqualTo(state.progress.level)
        assertThat(clearEnd.events).containsExactly(GameEvent.LinesCleared(LineClear(2)))
    }

    @Test
    fun `the level up comes after the lines cleared and before the stack danger of that tick`() {
        // Given a stack in danger at 8 lines and level 0, 14 rows high: a T clears two rows, which leaves it 12 high
        val state = twoRowsAboutToLock(Level(0), Lines(8)).copy(
            board = Board.parse((List(12) { ".........O" } + listOf("IIII...III", "IIIII.IIII")).joinToString("\n")),
            stackInDanger = true,
        )

        // When the piece locks and its clear is over, at 10 lines
        val clearEnd = engine.endOfClear(state)

        // Then
        assertThat(clearEnd.events).containsExactly(
            GameEvent.LinesCleared(LineClear(2)),
            GameEvent.LevelUp(Level(1)),
            GameEvent.StackDanger(on = false),
        )
    }

    @Test
    fun `starting at level 5 the first level up comes at 60 lines, not at 10`() {
        // Given a game started at level 5, and a T about to lock that fills two rows, at 8 lines and at 58 lines
        val at8 = twoRowsAboutToLock(Level(5), Lines(8))
        val at58 = twoRowsAboutToLock(Level(5), Lines(58))

        // When the piece locks and its clear is over, at 10 lines and at 60 lines
        val at10 = engine.endOfClear(at8)
        val at60 = engine.endOfClear(at58)

        // Then
        assertThat(at10.events).noneMatch { it is GameEvent.LevelUp }
        assertThat(at60.events).contains(GameEvent.LevelUp(Level(6)))
    }

    @ParameterizedTest(name = "start level {0}, {1} lines, then 2 more -> level {2}")
    @CsvSource("0, 290, 29", "9, 0, 9", "9, 97, 9", "9, 98, 10")
    fun `the level never goes down after a clear`(
        startLevel: Int,
        lines: Int,
        expectedLevel: Int,
    ) {
        // Given a game started at [startLevel] with [lines] lines, and a T about to lock that fills two rows
        val state = twoRowsAboutToLock(Level(startLevel), Lines(lines))

        // When the piece locks and its clear is over
        val cleared = engine.endOfClear(state).state

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

    /** The update of the tick that ends the clear started when the piece of [state] locks. */
    private fun GameEngine.endOfClear(state: GameState): GameUpdate = generateSequence(tick(state)) { tick(it.state) }
        .take(MAX_TICKS_TO_CLEAR_END)
        .first { it.state.phase is Phase.EntryDelay }

    private companion object {
        /** The lock, then the longest clear: 16 ticks plus 3 up to the next multiple of 4. */
        const val MAX_TICKS_TO_CLEAR_END = 20

        /**
         * A game started at [startLevel] with [lines] lines, a T about to lock on the next tick, whatever the
         * level, that fills the two rows of [TWO_ROWS_TO_FILL].
         */
        fun twoRowsAboutToLock(
            startLevel: Level,
            lines: Lines,
        ): GameState = gameState(
            board = TWO_ROWS_TO_FILL,
            startLevel = startLevel,
            lines = lines,
            phase = Phase.Falling(tPointingDown(at = Position(5, 18)), ticksSinceDrop = 47),
        )

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
