package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class WindowButtonLookTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("ch.magori.blox.core.designsystem.component.ButtonLookTest#variants")
    fun `a deleting answer is filled in danger with its trash icon, its label standing out by 4,5 to 1`(
        theme: String,
        variant: ButtonLookTest.Variant,
    ) {
        // Given the answer of a window that deletes
        val kind = WindowButtonKind.DANGER

        // When at rest and pressed
        val looks = listOf(0f, 1f).map { depth -> variant.look(kind.family, ButtonState(depth = depth)) }

        // Then filled in danger, its label in danger ink, beside the trash (WCAG 2.1, 1.4.3)
        assertThat(kind.icon).isEqualTo(BloxIcon.TRASH)
        assertThat(looks).allSatisfy { look ->
            assertThat(look.fill).isEqualTo(variant.palette.danger)
            assertThat(look.ink).isEqualTo(variant.palette.dangerInk)
            assertThat(contrast(look.ink, look.fill)).isGreaterThanOrEqualTo(4.5)
        }
    }
}
