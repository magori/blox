package ch.magori.blox.feature.garden

import ch.magori.blox.core.garden.GardenMood
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GardenViewModelTest {
    @Test
    fun `the garden opens empty and calm`() {
        // Given / When
        val viewModel = GardenViewModel()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.garden.pieces).isEmpty()
        assertThat(state.garden.mood).isEqualTo(GardenMood.CALM)
    }
}
