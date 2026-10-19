package ch.magori.blox.feature.home

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomeUiStateTest {
    private fun stateWith(startLevels: List<Int>) = HomeUiState(
        theme = ThemeChoice.ORIGINAL,
        music = MusicChoice.KOROBEINIKI,
        startLevel = startLevels.first(),
        startLevels = startLevels,
        record = 0,
        listenable = emptyList(),
        previewing = null,
    )

    @Test
    fun `the start levels 0 to 9 stand on two rows of five, 0 to 4 then 5 to 9`() {
        // Given
        val state = stateWith(startLevels = (0..9).toList())

        // When
        val rows = state.startLevelRows

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7, 8, 9))
    }

    @Test
    fun `a last row of fewer than five start levels holds those left, in order`() {
        // Given a mode of 8 start levels
        val state = stateWith(startLevels = (0..7).toList())

        // When
        val rows = state.startLevelRows

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7))
    }

    @Test
    fun `five start levels stand on a single full row`() {
        // Given
        val state = stateWith(startLevels = (0..4).toList())

        // When
        val rows = state.startLevelRows

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4))
    }

    @Test
    fun `eleven start levels stand on three rows, the last holding a single level`() {
        // Given
        val state = stateWith(startLevels = (0..10).toList())

        // When
        val rows = state.startLevelRows

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7, 8, 9), listOf(10))
    }
}
