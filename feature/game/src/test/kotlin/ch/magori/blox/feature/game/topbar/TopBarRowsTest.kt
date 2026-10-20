package ch.magori.blox.feature.game.topbar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/* The widths are in the same unit, whichever: pixels on the screen. */
class TopBarRowsTest {
    @Test
    fun `the title and the record stay on one row when each fits whole in half the room beside the pause button`() {
        // Given a room of 248 beside the pause button: 124 each
        val rows = topBarRows(room = 248f, titleWidth = 100f, recordWidth = 96f)

        // Then
        assertThat(rows).isEqualTo(TopBarRows.ONE)
    }

    @Test
    fun `the title goes above the pause button and the record when it does not fit whole in its half`() {
        // Given a room of 108 beside the pause button: 54 each, for a title of 72
        val rows = topBarRows(room = 108f, titleWidth = 72f, recordWidth = 48f)

        // Then
        assertThat(rows).isEqualTo(TopBarRows.TITLE_ABOVE)
    }

    @Test
    fun `the title goes above the pause button and the record when the record does not fit whole in its half`() {
        // Given a room of 108: 54 each, for a record of 55
        val rows = topBarRows(room = 108f, titleWidth = 48f, recordWidth = 55f)

        // Then
        assertThat(rows).isEqualTo(TopBarRows.TITLE_ABOVE)
    }

    @Test
    fun `a title one pixel narrower than its half stays on the row, a margin against the rounding of the row`() {
        // Given a room of 248: 124 each, for a title of 123 and a record of 123
        val rows = topBarRows(room = 248f, titleWidth = 123f, recordWidth = 123f)

        // Then
        assertThat(rows).isEqualTo(TopBarRows.ONE)
    }

    @Test
    fun `a title within the last pixel of its half goes above, rather than risk being cut`() {
        // Given a room of 248: 124 each, for a title of 123.5
        val rows = topBarRows(room = 248f, titleWidth = 123.5f, recordWidth = 96f)

        // Then
        assertThat(rows).isEqualTo(TopBarRows.TITLE_ABOVE)
    }

    @Test
    fun `a record within the last pixel of its half sends the title above, rather than risk being cut`() {
        // Given a room of 248: 124 each, for a record of 123.5
        val rows = topBarRows(room = 248f, titleWidth = 100f, recordWidth = 123.5f)

        // Then
        assertThat(rows).isEqualTo(TopBarRows.TITLE_ABOVE)
    }
}
