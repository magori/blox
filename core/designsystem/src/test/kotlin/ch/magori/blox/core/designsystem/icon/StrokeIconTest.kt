package ch.magori.blox.core.designsystem.icon

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.VectorPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class StrokeIconTest {
    @Test
    fun `a stroke icon is drawn on a grid of 24, in unfilled lines 2,25 thick, rounded at their ends and joins`() {
        // When
        val vector = strokeIcon(BloxIcon.HOME)

        // Then
        assertThat(vector.viewportWidth).isEqualTo(24f)
        assertThat(vector.viewportHeight).isEqualTo(24f)
        val path = vector.root.single() as VectorPath
        assertThat(path.fill).isNull()
        assertThat(path.strokeLineWidth).isEqualTo(2.25f)
        assertThat(path.strokeLineCap).isEqualTo(StrokeCap.Round)
        assertThat(path.strokeLineJoin).isEqualTo(StrokeJoin.Round)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxIcon::class)
    fun `every icon has strokes to draw`(icon: BloxIcon) {
        // When
        val path = strokeIcon(icon).root.single() as VectorPath

        // Then
        assertThat(path.pathData).isNotEmpty()
    }
}
