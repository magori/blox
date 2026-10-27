package ch.magori.blox.core.board

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoardRowsSizeTest {
    @Test
    fun `the bottom rows of the board take ten cells across and one cell per row`() {
        assertThat(boardRowsSize(rows = 6, cellSize = 14.dp)).isEqualTo(DpSize(140.dp, 84.dp))
    }
}
