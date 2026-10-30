package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.engine.Button
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngine
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.Input
import ch.magori.blox.core.game.engine.Phase
import ch.magori.blox.core.game.engine.ScriptedRandomizer
import ch.magori.blox.core.game.engine.gameState
import ch.magori.blox.core.game.engine.press
import ch.magori.blox.core.game.engine.ticks
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.rules.DropTiming
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TempoDropTimingTest {
    /** A meter of 15 ticks per eighth: 120 beats per minute. */
    private val dropTiming: DropTiming = TempoDropTiming(Meter(ticksPerEighth = Ticks(15)))

    @Test
    fun `the piece falls one row on every eighth`() {
        // Given a piece falling by gravity alone, from tick 1 to tick 60

        // When
        val dropTicks = ticksWhereThePieceDrops(1..60)

        // Then it drops on every eighth, and only then
        assertThat(dropTicks).containsExactly(15, 30, 45, 60)
    }

    @Test
    fun `a piece that appears between two eighths falls on the next one`() {
        // Given a piece that appeared on tick 21, between the eighths of ticks 15 and 30

        // When it falls by gravity alone from tick 22
        val dropTicks = ticksWhereThePieceDrops(22..60)

        // Then it drops on the next eighth, 9 ticks after it appeared, then on every eighth
        assertThat(dropTicks).containsExactly(30, 45, 60)
        // even when it appeared on tick 29, one tick before the eighth
        assertThat(ticksWhereThePieceDrops(30..60)).containsExactly(30, 45, 60)
    }

    @Test
    fun `the eighth does not drop the piece during a soft drop`() {
        // Given the soft drop held on the eighth of tick 30, at level 0

        // When
        val drops = dropTiming.dropsAfter(Ticks(1), tickCount = 30, Level(0), softDropping = true)

        // Then the gravity leaves the piece to the soft drop
        assertThat(drops).isFalse()
    }

    @ParameterizedTest(name = "level {0}")
    @ValueSource(ints = [0, 9, 19, 29])
    fun `the level does not change the fall`(level: Int) {
        // Given a piece falling by gravity alone at that level, from the slowest of the Classic to the fastest

        // When
        val dropTicks = ticksWhereThePieceDrops(1..60, Level(level))

        // Then it drops on every eighth, as at level 0
        assertThat(dropTicks).containsExactly(15, 30, 45, 60)
    }

    @Test
    fun `the soft drop stays the one of the Classic`() {
        // Given down pressed on a T at the top of the board at tick 0, with the rules of the Classic at level 0 and
        // with the fall on the eighths
        val classicEngine = GameEngine(ClassicRules, ScriptedRandomizer())
        val pressed = classicEngine.press(gameState(), Button.DOWN)

        // When down is held across the eighths of ticks 15 and 30, until the piece reaches the floor
        val classicDrops = ticksWhereThePieceDropped(classicEngine, pressed, ticks = 37)
        val tempoDrops = ticksWhereThePieceDropped(tempoEngine(), pressed, ticks = 37)

        // Then the piece drops one row every 2 ticks from the press, as in the Classic
        assertThat(tempoDrops).containsExactlyElementsOf((3..37 step 2).toList()).isEqualTo(classicDrops)
    }

    @Test
    fun `after a released soft drop the piece falls on the next eighth`() {
        // Given down held from tick 0 for 3 ticks, its step on tick 3, then released
        val engine = tempoEngine()
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN)
        val released = engine.apply(engine.ticks(pressed, 3), Input.Release(Button.DOWN)).state

        // When
        val dropTicks = ticksWhereThePieceDropped(engine, released, ticks = 27)

        // Then the gravity takes over on the next eighth, then on every eighth
        assertThat(dropTicks).containsExactly(15, 30)
    }

    @Test
    fun `a piece that appears on an eighth falls on the next one`() {
        // Given an entry delay that ends on tick 15, an eighth: the piece appears then
        val waiting = gameState(tickCount = 14, phase = Phase.EntryDelay(Ticks(1)))

        // When
        val dropTicks = ticksWhereThePieceDropped(tempoEngine(PieceType.O), waiting, ticks = 31)

        // Then it does not drop on the tick it appears, but on the next eighth, 15 ticks later
        assertThat(dropTicks).containsExactly(30, 45)
    }

    @Test
    fun `a held soft drop is not hurried by the eighths`() {
        // Given down pressed on a T falling at tick 1, its soft drop steps on the even ticks
        val engine = tempoEngine()
        val pressed = engine.press(gameState(tickCount = 1, phase = Phase.Falling(T_AT_5_10)), Button.DOWN)

        // When down is held across the eighth of tick 15, between two steps
        val dropTicks = ticksWhereThePieceDropped(engine, pressed, ticks = 15)

        // Then the eighth does not drop the piece: one row every 2 ticks, as in the Classic
        assertThat(dropTicks).containsExactly(4, 6, 8, 10, 12, 14, 16)
    }

    /**
     * When: a piece falls by gravity alone over [tickCounts], at [level], its count starting again at each drop; the
     * ticks where it drops.
     */
    private fun ticksWhereThePieceDrops(
        tickCounts: IntRange,
        level: Level = Level(0),
    ): List<Int> {
        var ticksSinceDrop = 0
        return tickCounts.filter { tickCount ->
            ticksSinceDrop += 1
            dropTiming.dropsAfter(Ticks(ticksSinceDrop), tickCount, level, softDropping = false)
                .also { if (it) ticksSinceDrop = 0 }
        }
    }

    /** When: [ticks] ticks of [engine] from [state]; the tick counts where the piece in play dropped one row. */
    private fun ticksWhereThePieceDropped(
        engine: GameEngine,
        state: GameState,
        ticks: Int,
    ): List<Int> = generateSequence(state) { engine.tick(it).state }
        .take(ticks + 1)
        .zipWithNext()
        .filter { (before, after) ->
            val (rowBefore, rowAfter) = fallingRow(before) to fallingRow(after)
            rowBefore != null && rowAfter != null && rowAfter != rowBefore
        }
        .map { (_, after) -> after.tickCount }
        .toList()

    /** The row of the piece falling in [state], null between two pieces: neither an appearance nor a lock is a drop. */
    private fun fallingRow(state: GameState): Int? = (state.phase as? Phase.Falling)?.piece?.pivot?.row

    /** The engine of the Classic, the piece falling on the eighths: the rules of Au tempo, before `TempoRules`. */
    private fun tempoEngine(vararg pieces: PieceType) =
        GameEngine(ClassicRules.copy(dropTiming = dropTiming), ScriptedRandomizer(*pieces))

    private companion object {
        val T_AT_5_10 = tPointingDown(at = Position(5, 10))
    }
}
