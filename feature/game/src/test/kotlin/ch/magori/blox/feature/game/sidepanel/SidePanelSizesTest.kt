package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SidePanelSizesTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(PieceType::class)
    fun `every next piece fits the preview the layout reserves, 4 cells wide and 2 high`(type: PieceType) {
        // When
        val next = nextPieceCells(type, ClassicRotationSystem)

        // Then
        assertThat(next.columns).isLessThanOrEqualTo(SidePanelSizes.PREVIEW_COLUMNS)
        assertThat(next.rows).isLessThanOrEqualTo(SidePanelSizes.PREVIEW_ROWS)
    }
}
