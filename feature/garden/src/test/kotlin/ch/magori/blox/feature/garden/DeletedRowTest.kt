package ch.magori.blox.feature.garden

import ch.magori.blox.core.designsystem.theme.BloxEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The row of a garden deleted from « Mes jardins » as it leaves the list, in ticks from the deletion (ADR 0024). */
class DeletedRowTest {
    @Test
    fun `a deleted row fades out, then folds up, in 12 ticks`() {
        // When, tick by tick from the deletion
        val row = (0..ROW_TICKS).map { deletedRow(it) }

        // Then whole at first; fading out at its height; then folding up, out of sight; gone at its 12th tick
        assertThat(deletedRowTicks(BloxEffects.Full)).isEqualTo(ROW_TICKS)
        assertThat(row.first()).isEqualTo(RowLeaving(alpha = 1f, height = 1f))
        assertThat(row.take(HALF + 1)).allSatisfy { assertThat(it.height).isEqualTo(1f) }
        assertThat(row.map { it.alpha }).isSortedAccordingTo(reverseOrder())
        assertThat(row.drop(HALF)).allSatisfy { assertThat(it.alpha).isZero() }
        assertThat(row.map { it.height }).isSortedAccordingTo(reverseOrder())
        assertThat(row[ROW_TICKS - 1].height).isGreaterThan(0f)
        assertThat(row.last()).isEqualTo(RowLeaving(alpha = 0f, height = 0f))
    }

    @Test
    fun `in reduced effects a deleted row is gone at once`() {
        // Then
        assertThat(deletedRowTicks(BloxEffects(reduced = true, forcedBySystem = false))).isZero()
    }

    private companion object {
        /** 200 ms; the fade in its first half, the fold in the second. */
        const val ROW_TICKS = 12
        const val HALF = 6
    }
}
