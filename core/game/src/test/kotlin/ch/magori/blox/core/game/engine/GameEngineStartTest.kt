package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class GameEngineStartTest {
    @Test
    fun `a game started at level 0 has level 0, no line, no point, an empty board and a falling piece`() {
        // Given
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.T, PieceType.J))

        // When
        val state = engine.start(Level(0)).state

        // Then
        assertThat(state.startLevel).isEqualTo(Level(0))
        assertThat(state.level).isEqualTo(Level(0))
        assertThat(state.lines).isEqualTo(Lines(0))
        assertThat(state.score).isEqualTo(Points(0))
        assertThat(state.board).isEqualTo(Board.empty())
        assertThat(state.phase).isInstanceOf(Phase.Falling::class.java)
    }

    @Test
    fun `starting a game gives no event`() {
        // Given
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.T, PieceType.J))

        // When
        val events = engine.start(Level(0)).events

        // Then
        assertThat(events).isEmpty()
    }

    @Test
    fun `the last starting level of the range is accepted`() {
        // Given
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.T, PieceType.J))

        // When
        val state = engine.start(Level(9)).state

        // Then
        assertThat(state.startLevel).isEqualTo(Level(9))
        assertThat(state.level).isEqualTo(Level(9))
    }

    @ParameterizedTest(name = "level {0}")
    @ValueSource(ints = [10, 19])
    fun `a starting level outside the range of the level progression is refused`(level: Int) {
        // Given
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.T, PieceType.J))

        // When / Then
        assertThatIllegalArgumentException()
            .isThrownBy { engine.start(Level(level)) }
            .withMessageContaining("start level")
    }

    @Test
    fun `the first piece is drawn with no previous piece, the next one with the first as previous`() {
        // Given
        val randomizer = ScriptedRandomizer(PieceType.T, PieceType.J)
        val engine = GameEngine(ClassicRules, randomizer)

        // When
        val state = engine.start(Level(0)).state

        // Then
        assertThat(state.current?.type).isEqualTo(PieceType.T)
        assertThat(state.next).isEqualTo(PieceType.J)
        assertThat(randomizer.previousArguments).containsExactly(null, PieceType.T)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(PieceType::class)
    fun `the piece in play appears with its pivot at (5, 0), in the spawn orientation of the rotation system`(
        type: PieceType,
    ) {
        // Given
        val engine = GameEngine(ClassicRules, ScriptedRandomizer(type, PieceType.J))

        // When
        val state = engine.start(Level(0)).state

        // Then
        assertThat(state.current).isEqualTo(spawned(type))
    }
}
