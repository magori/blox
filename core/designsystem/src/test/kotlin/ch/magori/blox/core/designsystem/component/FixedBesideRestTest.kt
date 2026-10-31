package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FixedBesideRestTest {
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
}
