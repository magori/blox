package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.rules.ClassicLineClearDelay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RulesTest {
    @Test
    fun `the Classic rules clear lines with the line clear delay of the NES`() {
        assertThat(ClassicRules.lineClearDelay).isSameAs(ClassicLineClearDelay)
    }
}
