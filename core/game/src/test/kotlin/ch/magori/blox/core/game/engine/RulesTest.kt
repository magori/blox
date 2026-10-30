package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.rules.ClassicDropTiming
import ch.magori.blox.core.game.rules.ClassicGravityTable
import ch.magori.blox.core.game.rules.ClassicLineClearDelay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RulesTest {
    @Test
    fun `the Classic rules clear lines with the line clear delay of the NES`() {
        assertThat(ClassicRules.lineClearDelay).isSameAs(ClassicLineClearDelay)
    }

    @Test
    fun `rules built again with the classic drop timing are the classic rules`() {
        // Given / When
        val rebuilt = ClassicRules.copy(dropTiming = ClassicDropTiming(ClassicGravityTable))

        // Then
        assertThat(rebuilt).isEqualTo(ClassicRules)
    }
}
