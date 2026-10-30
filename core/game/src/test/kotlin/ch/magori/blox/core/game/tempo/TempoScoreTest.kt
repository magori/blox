package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.engine.Button
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngine
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.GameUpdate
import ch.magori.blox.core.game.engine.Phase
import ch.magori.blox.core.game.engine.ScriptedRandomizer
import ch.magori.blox.core.game.engine.TWO_ROWS_TO_FILL
import ch.magori.blox.core.game.engine.gameState
import ch.magori.blox.core.game.engine.press
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.RowsCleared
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TempoScoreTest {
    /** A meter of 15 ticks per eighth, 120 beats per minute: a quarter note every 30 ticks, a phrase every 480. */
    private val score = TempoScore(Meter(ticksPerEighth = Ticks(15)))

    @Test
    fun `a piece locked on the beat by the held soft drop earns 80 points`() {
        // Given a piece locked by the soft drop on tick 32, 2 ticks after beat 2 of bar 1
        val (before, lock) = lock(tickCount = 32, bySoftDrop = true)

        // When
        val update = score.after(before, lock)

        // Then
        assertThat(update.score.bonus).isEqualTo(Points(80))
        assertThat(update.events).containsExactly(TempoEvent.OnBeat(Points(80)))
    }

    @Test
    fun `a piece locked by gravity alone earns no beat bonus`() {
        // Given a piece locked by the gravity on tick 30, beat 2 of bar 1
        val (before, lock) = lock(tickCount = 30, bySoftDrop = false)

        // When
        val update = score.after(before, lock)

        // Then
        assertThat(update.score.bonus).isEqualTo(Points(0))
        assertThat(update.events).isEmpty()
    }

    @Test
    fun `a piece locked out of the window earns nothing`() {
        // Given a piece locked by the soft drop on tick 36, 6 ticks after beat 2 of bar 1, out of the window of 5 ticks
        val (before, lock) = lock(tickCount = 36, bySoftDrop = true)

        // When
        val update = score.after(before, lock)

        // Then
        assertThat(update.score.bonus).isEqualTo(Points(0))
        assertThat(update.events).isEmpty()
    }

    @Test
    fun `lines cleared on the beat in bar 4 earn their points twice`() {
        // Given a piece locked by the soft drop on tick 452, 2 ticks after beat 4 of bar 4, then its 2 lines cleared on
        // tick 470, for 100 points of the engine
        val (beforeLock, lock) = lock(tickCount = 452, bySoftDrop = true)
        val locked = score.after(beforeLock, lock).score
        val (beforeClear, clear) = clear(tickCount = 470, lines = 2, points = 100)

        // When
        val update = locked.after(beforeClear, clear)

        // Then the clear earns its 100 points once more, on top of the 80 points of the lock
        assertThat(update.score.bonus).isEqualTo(Points(180))
        assertThat(update.events).containsExactly(TempoEvent.CadenceCleared(Points(100)))
    }

    @Test
    fun `lines of a piece locked off the beat in bar 4 earn no cadence bonus`() {
        // Given a piece locked by the soft drop on tick 460, in bar 4 but 10 ticks from beat 4, then its lines cleared
        val (beforeLock, lock) = lock(tickCount = 460, bySoftDrop = true)
        val locked = score.after(beforeLock, lock).score
        val (beforeClear, clear) = clear(tickCount = 478, lines = 2, points = 100)

        // When
        val update = locked.after(beforeClear, clear)

        // Then
        assertThat(update.score.bonus).isEqualTo(Points(0))
        assertThat(update.events).isEmpty()
    }

    @Test
    fun `an on-beat lock without lines is forgotten by the next lock`() {
        // Given a piece locked on the beat in bar 4 on tick 452, without lines, then a piece locked off the beat on
        // tick 460, whose lines are cleared on tick 478
        val (beforeOnBeat, onBeat) = lock(tickCount = 452, bySoftDrop = true)
        val (beforeOffBeat, offBeat) = lock(tickCount = 460, bySoftDrop = true)
        val locked = score.after(beforeOnBeat, onBeat).score.after(beforeOffBeat, offBeat).score
        val (beforeClear, clear) = clear(tickCount = 478, lines = 2, points = 100)

        // When
        val update = locked.after(beforeClear, clear)

        // Then the lines are those of the lock off the beat: no cadence
        assertThat(update.events).isEmpty()
        assertThat(update.score.bonus).isEqualTo(Points(80))
    }

    @Test
    fun `lines locked in bar 4 and cleared in bar 1 still make a cadence`() {
        // Given a piece locked by the soft drop on tick 455, 5 ticks after beat 4 of bar 4, its lines cleared on tick
        // 490, in bar 1 of the next phrase
        val (beforeLock, lock) = lock(tickCount = 455, bySoftDrop = true)
        val locked = score.after(beforeLock, lock).score
        val (beforeClear, clear) = clear(tickCount = 490, lines = 1, points = 40)

        // When
        val update = locked.after(beforeClear, clear)

        // Then the cadence is the one of the lock: its 40 points once more, on top of the 80 points of the lock
        assertThat(update.events).containsExactly(TempoEvent.CadenceCleared(Points(40)))
        assertThat(update.score.bonus).isEqualTo(Points(80 + 40))
    }

    @Test
    fun `a piece locked 5 ticks before bar 1 is not in bar 4`() {
        // Given a piece locked by the soft drop on tick 475, in bar 4 but on beat 1 of bar 1, 5 ticks before it, then
        // its lines cleared
        val (beforeLock, lock) = lock(tickCount = 475, bySoftDrop = true)
        val locked = score.after(beforeLock, lock)
        val (beforeClear, clear) = clear(tickCount = 495, lines = 2, points = 100)

        // When
        val update = locked.score.after(beforeClear, clear)

        // Then the lock is on the beat, but not a cadence
        assertThat(locked.events).containsExactly(TempoEvent.OnBeat(Points(80)))
        assertThat(update.events).isEmpty()
    }

    @Test
    fun `the total is the points of the engine plus the bonuses`() {
        // Given two pieces locked on the beat by the soft drop, then lines cleared at 1 100 points of the engine
        val (beforeFirst, first) = lock(tickCount = 32, bySoftDrop = true)
        val (beforeSecond, second) = lock(tickCount = 62, bySoftDrop = true)
        val twoOnBeat = score.after(beforeFirst, first).score.after(beforeSecond, second).score
        val (_, clear) = clear(tickCount = 80, lines = 2, points = 100)

        // When
        val total = twoOnBeat.total(clear.state)

        // Then
        assertThat(total).isEqualTo(Points(POINTS_BEFORE + 100 + 80 + 80))
    }

    @Test
    fun `the pieces on the beat are counted against every piece locked`() {
        // Given pieces locked on the beat by the soft drop, by the gravity, and out of the window
        val locks = listOf(lock(32, bySoftDrop = true), lock(60, bySoftDrop = false), lock(76, bySoftDrop = true))

        // When
        val counted = locks.fold(score) { current, (before, lock) -> current.after(before, lock).score }

        // Then
        assertThat(counted.piecesOnBeat).isEqualTo(1)
        assertThat(counted.piecesLocked).isEqualTo(3)
    }

    @Test
    fun `a soft drop timed on beat 4 of bar 4 earns the beat bonus and a cadence in a real game`() {
        // Given the engine of the Classic falling on the eighths, a T above two rows it fills, and down pressed on tick
        // 431, so that the soft drop takes it to the floor and locks it on tick 450, beat 4 of bar 4
        val engine = GameEngine(ClassicRules.copy(dropTiming = TempoDropTiming(score.meter)), ScriptedRandomizer())
        val falling = gameState(board = TWO_ROWS_TO_FILL, tickCount = 431, phase = Phase.Falling(T_AT_5_10))
        val pressed = engine.press(falling, Button.DOWN)

        // When the game is played tick by tick, each tick fed to the score, until the lines are cleared
        val played = generateSequence(Played(pressed, TempoUpdate(score, emptyList()), cleared = false)) { last ->
            val update = engine.tick(last.state)
            val tempo = last.tempo.score.after(last.state, update)
            Played(update.state, TempoUpdate(tempo.score, last.tempo.events + tempo.events), update.cleared())
        }.first { it.cleared }

        // Then the lock is on the beat, and its 2 lines, 100 points of the engine at level 0, earn them once more
        assertThat(played.tempo.events)
            .containsExactly(TempoEvent.OnBeat(Points(80)), TempoEvent.CadenceCleared(Points(100)))
        assertThat(played.tempo.score.total(played.state)).isEqualTo(played.state.progress.score + Points(80 + 100))
    }

    /** A game played with its score: the last [state], the [tempo] score with every event so far, and if [cleared]. */
    private data class Played(val state: GameState, val tempo: TempoUpdate, val cleared: Boolean)

    private fun GameUpdate.cleared(): Boolean = events.any { it is GameEvent.LinesCleared }

    /**
     * Given: [lines] cleared on [tickCount], at the end of their animation, for [points] of the engine; the state
     * before that tick, and the update of the tick.
     */
    private fun clear(
        tickCount: Int,
        lines: Int,
        points: Int,
    ): Pair<GameState, GameUpdate> {
        val rowsCleared = RowsCleared(Board.empty(), clearedRows = (BOTTOM_ROW - lines + 1..BOTTOM_ROW).toList())
        val clearing = Phase.Clearing(rowsCleared, Ticks(1), entryDelayAfter = Ticks(10), stackInDangerAfter = false)
        val before = gameState(tickCount = tickCount - 1, phase = clearing)
            .let { it.copy(progress = it.progress.withPoints(Points(POINTS_BEFORE))) }
        val cleared = before.copy(
            tickCount = tickCount,
            progress = before.progress.withPoints(Points(points)),
            phase = Phase.EntryDelay(Ticks(10)),
        )
        return before to GameUpdate(cleared, listOf(GameEvent.LinesCleared(LineClear(lines))))
    }

    /**
     * Given: a T locking on [tickCount] (the tick being played, already counted), by the soft drop if [bySoftDrop],
     * else by the gravity; the state before that tick, and the update of the tick.
     */
    private fun lock(
        tickCount: Int,
        bySoftDrop: Boolean,
    ): Pair<GameState, GameUpdate> {
        val before = gameState(tickCount = tickCount - 1, phase = Phase.Falling(T_ON_THE_FLOOR))
        val locked = before.copy(tickCount = tickCount, phase = Phase.EntryDelay(Ticks(10)))
        return before to GameUpdate(locked, listOf(GameEvent.PieceLocked(T_ON_THE_FLOOR, bySoftDrop)))
    }

    private companion object {
        val T_ON_THE_FLOOR = tPointingDown(at = Position(5, 18))
        val T_AT_5_10 = tPointingDown(at = Position(5, 10))

        /** The points of the engine before a clear: not 0, so that the points of the clear are read as a difference. */
        const val POINTS_BEFORE = 1000

        /** The bottom row of the board, from which the cleared rows are counted. */
        const val BOTTOM_ROW = 19
    }
}
