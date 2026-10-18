package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameEngineFactoryTest {
    private val factory: GameEngineFactory = ClassicGameEngineFactory

    @Test
    fun `two engines created with the same seed deal the same pieces`() {
        // When
        val first = factory.create(seed = 42).start(Level(0)).state
        val second = factory.create(seed = 42).start(Level(0)).state

        // Then
        assertThat(second.current).isEqualTo(first.current)
        assertThat(second.next).isEqualTo(first.next)
    }
}
