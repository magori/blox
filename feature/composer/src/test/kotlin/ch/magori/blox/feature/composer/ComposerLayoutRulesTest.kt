package ch.magori.blox.feature.composer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ComposerLayoutRulesTest {
    @Test
    fun `the grid takes the room the parts leave on the page, its least height at least`() {
        assertThat(gridHeight(page = 1000, taken = 600, gridMin = 200)).isEqualTo(400)
        assertThat(gridHeight(page = 700, taken = 600, gridMin = 200)).isEqualTo(200)
    }

    @Test
    fun `on a page of no bounded height the grid keeps its least height`() {
        assertThat(gridHeight(page = null, taken = 600, gridMin = 200)).isEqualTo(200)
    }
}
