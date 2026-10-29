package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.locksmith.PadlockResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The summary of the screen « Serrures » and the padlock Reprendre opens. */
class PadlocksUiStateTest {
    @Test
    fun `the summary counts the open padlocks and the perfect ones, and Reprendre opens the next`() {
        // Given padlocks 1 to 3 open, 1 and 3 perfectly
        val perfect = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val withAHint = PadlockResult(pieces = 4, moves = 4, refusals = 0, hints = 1)
        val state = PadlocksUiState(listOf(perfect, withAHint, perfect.copy(pieces = 5, moves = 5)))

        // Then
        assertThat(state.open).isEqualTo(3)
        assertThat(state.perfect).isEqualTo(2)
        assertThat(state.frontier).isEqualTo(4)
    }

    @Test
    fun `without an opening the screen is empty, and padlock 1 is to open`() {
        // Given
        val state = PadlocksUiState(emptyList())

        // Then
        assertThat(state.isEmpty).isTrue
        assertThat(state.frontier).isEqualTo(1)
    }
}
