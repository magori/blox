package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/*
 * On the JVM, `Density(density, fontScale)` has no `FontScaleConverter`: only the linear font scaling is exercised
 * here. The non-linear scaling of Android 14 and later goes through `toSp()` in `shrinkFloor`, but is not tested
 * here.
 */
class ShrinkFloorTest {
    @ParameterizedTest(name = "{0} sp at a font scale of {1}")
    @MethodSource("sizesAndScales")
    fun `the floor is never above the largest size of the text`(
        maxFontSize: Float,
        fontScale: Float,
    ) {
        val density = Density(density = 2.625f, fontScale = fontScale)

        assertThat(density.shrinkFloor(maxFontSize.sp).value).isLessThanOrEqualTo(maxFontSize)
    }

    @ParameterizedTest(name = "{0} sp at a font scale of {1}")
    @MethodSource("sizesAndScales")
    fun `the floor is never smaller on screen than 8 sp or than the text with the normal font, whichever is smaller`(
        maxFontSize: Float,
        fontScale: Float,
    ) {
        val density = Density(density = 2.625f, fontScale = fontScale)

        val floor = with(density) { density.shrinkFloor(maxFontSize.sp).toDp().value }

        val minimum = minOf(with(density) { 8.sp.toDp().value }, maxFontSize)
        assertThat(floor).isGreaterThanOrEqualTo(minimum - TOLERANCE)
    }

    @Test
    fun `with a font enlarged twice a label of 11 sp may shrink back to its 11 dp with the normal font`() {
        val density = Density(density = 2.625f, fontScale = 2f)

        val floor = density.shrinkFloor(maxFontSize = 11.sp)

        assertThat(floor).isEqualTo(5.5.sp)
        assertThat(with(density) { floor.toDp() }.value).isEqualTo(11f)
    }

    @Test
    fun `with a font made smaller a text smaller than 8 sp never gets a floor above its largest size`() {
        val density = Density(density = 2.625f, fontScale = 0.85f)

        assertThat(density.shrinkFloor(maxFontSize = 7.sp)).isEqualTo(7.sp)
    }

    @Test
    fun `with a font enlarged twice a text not enlarged keeps its 18 dp with the normal font`() {
        val density = Density(density = 2.625f, fontScale = 2f)

        val size = density.notEnlarged(18.sp)

        assertThat(size).isEqualTo(9.sp)
        assertThat(with(density) { size.toDp() }.value).isEqualTo(18f)
    }

    @Test
    fun `with a font made smaller a text not enlarged keeps its size`() {
        val density = Density(density = 2.625f, fontScale = 0.85f)

        assertThat(density.notEnlarged(18.sp)).isEqualTo(18.sp)
    }

    @ParameterizedTest(name = "{0} sp at a font scale of {1}")
    @MethodSource("sizesAndScales")
    fun `the floor is never above the size of the text not enlarged`(
        maxFontSize: Float,
        fontScale: Float,
    ) {
        val density = Density(density = 2.625f, fontScale = fontScale)

        val floor = density.shrinkFloor(maxFontSize.sp).value

        assertThat(floor).isLessThanOrEqualTo(density.notEnlarged(maxFontSize.sp).value)
    }

    private companion object {
        /** The rounding of the conversions between sp and dp, in floats. */
        const val TOLERANCE = 0.001f

        /** The sizes of the label, the title and the values, at the font scales Android offers. */
        @JvmStatic
        fun sizesAndScales(): List<Arguments> = listOf(11f, 14f, 18f).flatMap { size ->
            listOf(0.85f, 1f, 1.3f, 1.5f, 2f).map { scale -> Arguments.of(size, scale) }
        }
    }
}
