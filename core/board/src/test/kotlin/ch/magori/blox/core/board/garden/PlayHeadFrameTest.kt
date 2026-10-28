package ch.magori.blox.core.board.garden

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlayHeadFrameTest {
    @Test
    fun `the frame of the play head closes the column at its top cell and runs on below it`() {
        // When
        val frame = playHeadFrame(GardenPlayHead.TOP, side = 22f)

        // Then from the top edge of the cell to a cell below it: the bottom corners, out of the cell, are not drawn
        assertThat(frame).isEqualTo(PlayHeadFrame(top = 0f, bottom = 44f))
    }

    @Test
    fun `the frame of the play head runs on above and below a cell in the middle of the column`() {
        // When
        val frame = playHeadFrame(GardenPlayHead.MIDDLE, side = 22f)

        // Then only both sides of the cell are drawn
        assertThat(frame).isEqualTo(PlayHeadFrame(top = -22f, bottom = 44f))
    }

    @Test
    fun `the frame of the play head closes the column at its bottom cell and runs on above it`() {
        // When
        val frame = playHeadFrame(GardenPlayHead.BOTTOM, side = 22f)

        // Then
        assertThat(frame).isEqualTo(PlayHeadFrame(top = -22f, bottom = 22f))
    }
}
