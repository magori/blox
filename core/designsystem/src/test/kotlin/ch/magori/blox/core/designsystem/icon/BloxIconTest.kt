package ch.magori.blox.core.designsystem.icon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BloxIconTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxIcon::class)
    fun `every icon has a pixel drawing of 8 lines of 8 pixels, each full or empty`(icon: BloxIcon) {
        // Then
        assertThat(icon.pixels).hasSize(8).allSatisfy { line -> assertThat(line).matches("[#.]{8}") }
    }
}
