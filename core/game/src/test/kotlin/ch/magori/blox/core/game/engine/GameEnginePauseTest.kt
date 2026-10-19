package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.cellsOf
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.model.tPointingDown
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class GameEnginePauseTest {
    private val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.J))

    @Test
    fun `Pause pauses the game, without changing the phase or sending an event`() {
        // Given
        val state = gameState(phase = Phase.Falling(T_AT_5_10, ticksSinceDrop = 20))

        // When
        val paused = engine.apply(state, Input.Pause)

        // Then
        assertThat(paused.state.paused).isTrue()
        assertThat(paused.state.phase).isEqualTo(state.phase)
        assertThat(paused.events).isEmpty()
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("phases")
    fun `while paused a tick changes nothing, the tick count included`(phase: Phase) {
        // Given
        val paused = engine.apply(gameState(tickCount = 100, phase = phase), Input.Pause).state

        // When
        val ticked = generateSequence(engine.tick(paused)) { engine.tick(it.state) }.take(30).toList()

        // Then
        assertThat(ticked).allSatisfy { update ->
            assertThat(update.state).isEqualTo(paused)
            assertThat(update.events).isEmpty()
        }
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("phases")
    fun `Resume goes on exactly where the game stood`(phase: Phase) {
        // Given a game paused for 30 ticks
        val state = gameState(tickCount = 100, phase = phase)
        val paused = engine.ticks(engine.apply(state, Input.Pause).state, 30)

        // When
        val resumed = engine.apply(paused, Input.Resume)

        // Then
        assertThat(resumed.state).isEqualTo(state)
        assertThat(resumed.events).isEmpty()
    }

    @Test
    fun `a press while paused is ignored, so A pressed during the pause does not turn the piece`() {
        // Given
        val paused = engine.apply(gameState(phase = Phase.Falling(T_AT_5_10)), Input.Pause).state

        // When
        val resumed = engine.apply(engine.press(paused, Button.A), Input.Resume).state
        val ticked = engine.tick(resumed).state

        // Then
        assertThat(ticked.current).isEqualTo(T_AT_5_10)
    }

    @Test
    fun `the pause forgets the held buttons and the shift repeat, a release during the pause does nothing`() {
        // Given left held for 10 ticks (one shift, to column 4), then the game paused and left released
        val leftHeld = engine.ticks(engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.LEFT), 10)
        val paused = engine.apply(leftHeld, Input.Pause).state
        val released = engine.apply(paused, Input.Release(Button.LEFT))

        // When the game resumes, then left is pressed again after 20 ticks
        val resumed = engine.ticks(engine.apply(released.state, Input.Resume).state, 20)
        val pressedAgain = engine.press(resumed, Button.LEFT)

        // Then nothing moved until the new press, which starts a fresh repeat: steps on ticks 0 and 16
        assertThat(paused.controls).isEqualTo(Controls.NONE)
        assertThat(released.state).isEqualTo(paused)
        assertThat(resumed.current?.pivot).isEqualTo(Position(4, 10))
        val columns = generateSequence(pressedAgain) {
            engine.tick(it).state
        }.take(18).map { it.current?.pivot?.column }
        assertThat(columns.toList()).containsExactly(4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2)
    }

    @Test
    fun `the pause stops the soft drop, which needs a new press of down after the resume`() {
        // Given down held for 3 ticks (one row dropped), then the game paused and resumed
        val dropping = engine.ticks(engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN), 3)
        val resumed = engine.apply(engine.apply(dropping, Input.Pause).state, Input.Resume).state

        // When down is pressed again after 6 ticks, and held until the T locks on the floor
        val afterResume = engine.ticks(resumed, 6)
        val afterNewPress = engine.ticks(engine.press(afterResume, Button.DOWN), 3)
        val lock = generateSequence(engine.tick(afterNewPress)) { engine.tick(it.state) }
            .take(20)
            .first { it.events.isNotEmpty() }

        // Then only the 7 rows of the new soft drop, from row 11 to row 18, score: not the row before the pause
        assertThat(dropping.current?.pivot).isEqualTo(Position(5, 11))
        assertThat(afterResume.current?.pivot).isEqualTo(Position(5, 11))
        assertThat(afterNewPress.current?.pivot).isEqualTo(Position(5, 12))
        assertThat(lock.events).containsExactly(GameEvent.PieceLocked(tPointingDown(at = Position(5, 18))))
        assertThat(lock.state.progress.score).isEqualTo(Points(7))
    }

    @Test
    fun `a press just before the pause is forgotten`() {
        // Given A pressed, then the game paused before any tick, and resumed
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.A)
        val resumed = engine.apply(engine.apply(pressed, Input.Pause).state, Input.Resume).state

        // When
        val ticked = engine.tick(resumed).state

        // Then the piece did not turn
        assertThat(ticked.current).isEqualTo(T_AT_5_10)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("inputsWithoutEffect")
    fun `Pause while paused, Resume while playing and Pause after the end change nothing`(
        state: GameState,
        input: Input,
    ) {
        // When
        val update = engine.apply(state, input)

        // Then
        assertThat(update.state).isEqualTo(state)
        assertThat(update.events).isEmpty()
    }

    private companion object {
        val T_AT_5_10 = tPointingDown(at = Position(5, 10))

        @JvmStatic
        fun phases(): List<Arguments> = listOf(
            argumentSet("falling, the gravity count at 20", Phase.Falling(T_AT_5_10, ticksSinceDrop = 20)),
            argumentSet(
                "clearing, 12 ticks left",
                Phase.Clearing(
                    rowsCleared = Board.parse("IIIIIIIIII").clearFullRows(),
                    ticksLeft = Ticks(12),
                    entryDelayAfter = Ticks(10),
                    stackInDangerAfter = false,
                ),
            ),
            argumentSet("entry delay, 5 ticks left", Phase.EntryDelay(ticksLeft = Ticks(5))),
        )

        @JvmStatic
        fun inputsWithoutEffect(): List<Arguments> {
            val playing = gameState(phase = Phase.Falling(T_AT_5_10))
            val paused = gameState(phase = Phase.Falling(T_AT_5_10), paused = true)
            val over = gameState(phase = Phase.GameOver(spawned(PieceType.T)))
            return listOf(
                argumentSet("Pause while paused", paused, Input.Pause),
                argumentSet("Resume while playing", playing, Input.Resume),
                argumentSet("Pause after the end of the game", over, Input.Pause),
            )
        }
    }
}
