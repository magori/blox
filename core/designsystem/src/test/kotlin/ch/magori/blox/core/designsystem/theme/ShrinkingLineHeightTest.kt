package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ShrinkingLineHeightTest {
    @Test
    fun `a text that shrinks keeps the line height of its style in proportion to its size`() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)

        assertThat(style.shrinkingLineHeight().lineHeight).isEqualTo(1.5.em)
    }

    @Test
    fun `a style without a line height in sp keeps its own`() {
        val unspecified = TextStyle(fontSize = 14.sp)
        val inEm = TextStyle(fontSize = 14.sp, lineHeight = 1.2.em)

        assertThat(unspecified.shrinkingLineHeight().lineHeight).isEqualTo(TextUnit.Unspecified)
        assertThat(inEm.shrinkingLineHeight().lineHeight).isEqualTo(1.2.em)
    }
}
