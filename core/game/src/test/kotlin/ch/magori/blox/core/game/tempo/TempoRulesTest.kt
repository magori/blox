package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.model.Ticks
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TempoRulesTest {
    @Test
    fun `tempo rules are the classic rules but for the drop timing`() {
        // Given a meter of 12 ticks per eighth: 150 beats per minute
        val meter = Meter(ticksPerEighth = Ticks(12))

        // When
        val rules = tempoRules(meter)

        // Then the piece falls on the eighths of that meter, and nothing else differs from the Classic
        assertThat(rules.dropTiming).isEqualTo(TempoDropTiming(meter))
        assertThat(rules.copy(dropTiming = ClassicRules.dropTiming)).isEqualTo(ClassicRules)
    }
}
