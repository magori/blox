package ch.magori.blox.feature.composer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ComposerLayoutRulesTest {
    @Test
    fun `a part stands beside the fixed one while the rest of the line holds what it needs, to the pixel`() {
        // Given a line of 100 px, 4 px of gap, a fixed part of 40 px: 56 px left
        // Then
        assertThat(fitsBesideFixed(width = 100, gap = 4, fixedWidth = 40, restNeeds = 56)).isTrue()
        assertThat(fitsBesideFixed(width = 100, gap = 4, fixedWidth = 40, restNeeds = 57)).isFalse()
    }

    @Test
    fun `a fixed part wider than the line leaves nothing beside it`() {
        assertThat(fitsBesideFixed(width = 100, gap = 4, fixedWidth = 150, restNeeds = 0)).isFalse()
    }

    @Test
    fun `parts stand side by side when the widest holds in its share of the line less the gaps`() {
        // Given two parts of 48 px at most, 4 px apart: a share of 48 px needs 100 px
        assertThat(sideBySide(width = 100, gap = 4, count = 2, widest = 48)).isTrue()
        assertThat(sideBySide(width = 99, gap = 4, count = 2, widest = 48)).isFalse()
    }

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
