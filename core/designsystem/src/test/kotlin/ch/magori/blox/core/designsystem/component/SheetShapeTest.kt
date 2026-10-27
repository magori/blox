package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxShapeSets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SheetShapeTest {
    @Test
    fun `a sheet takes the corners of a card at its top, and square ones at its bottom, on the edge of the screen`() {
        // Given the cards of Couleurs, of 28 dp
        val card = BloxShapeSets.Colorful.card

        // When
        val sheet = sheetShape(card)

        // Then
        assertThat(sheet).isEqualTo(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
    }
}
