package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.glossesOf
import ch.magori.blox.core.designsystem.theme.paletteOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * The looks of a switch, in the variants of Colorful alone: a setting row is drawn only on the settings page, which is
 * in Colorful whatever the theme chosen (P12.4, P12.13).
 */
internal class SwitchLookTest {
    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `a switch on has its thumb on the right, larger, checked, a switch off on the left`(variant: BloxThemeVariant) {
        // When
        val on = switchLook(variant, SwitchState(checked = true))
        val off = switchLook(variant, SwitchState(checked = false))

        // Then the state reads by the place, the size and the check of the thumb, not by colour alone (WCAG 1.4.1)
        assertThat(on.thumbAtEnd).isTrue()
        assertThat(on.thumbSize).isEqualTo(DpSize(24.dp, 24.dp))
        assertThat(on.check).isNotNull()
        assertThat(off.thumbAtEnd).isFalse()
        assertThat(off.thumbSize).isEqualTo(DpSize(20.dp, 20.dp))
        assertThat(off.check).isNull()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `a pressed row fills with button pressed and its thumb grows to 28 dp`(variant: BloxThemeVariant) {
        // Given
        val palette = paletteOf(variant)

        // When
        val atRest = switchLook(variant, SwitchState(checked = false))
        val pressedOff = switchLook(variant, SwitchState(checked = false, depth = 1f))
        val pressedOn = switchLook(variant, SwitchState(checked = true, depth = 1f))

        // Then the thumb grows along the track, as high as it was
        assertThat(atRest.rowFill).isEqualTo(Color.Transparent)
        assertThat(listOf(pressedOff.rowFill, pressedOn.rowFill)).containsOnly(palette.buttonPressed)
        assertThat(pressedOff.thumbSize).isEqualTo(DpSize(28.dp, 20.dp))
        assertThat(pressedOn.thumbSize).isEqualTo(DpSize(28.dp, 24.dp))
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `the track is framed in button outline at rest and in the ink of its row pressed, by 3 to 1, on and off`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val palette = paletteOf(variant)

        listOf(true, false).forEach { checked ->
            // When
            val atRest = switchLook(variant, SwitchState(checked))
            val pressed = switchLook(variant, SwitchState(checked, depth = 1f))

            // Then (WCAG 2.1, 1.4.11): the frame, not the accent, makes the 3:1 of the track, on the page at rest and
            // on the fill of its row pressed, whatever its state
            assertThat(atRest.trackFrame).isEqualTo(palette.buttonOutline)
            assertThat(contrast(atRest.trackFrame, palette.page)).isGreaterThanOrEqualTo(3.0)
            assertThat(pressed.trackFrame).isEqualTo(pressed.label)
            assertThat(contrast(pressed.trackFrame, pressed.rowFill)).isGreaterThanOrEqualTo(3.0)
        }
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `a focused row bears a 3 dp ring in ink, 3 dp off its edge`(variant: BloxThemeVariant) {
        // When
        val focused = switchLook(variant, SwitchState(checked = false, focused = true))
        val notFocused = switchLook(variant, SwitchState(checked = false))

        // Then (WCAG 2.2, 2.4.7), as a focused button
        assertThat(focused.focusRing).isEqualTo(FocusRing(paletteOf(variant).ink, 3.dp, 3.dp))
        assertThat(notFocused.focusRing).isNull()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `the thumb stands out from its track by 3 to 1, on and off, and the check from the thumb`(
        variant: BloxThemeVariant,
    ) {
        // When
        val on = switchLook(variant, SwitchState(checked = true))
        val off = switchLook(variant, SwitchState(checked = false))

        // Then (WCAG 2.1, 1.4.11)
        assertThat(contrast(on.thumb, on.track)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(off.thumb, off.track)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(on.check!!, on.thumb)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `the label and the sub-line stand out from their row by 4,5 to 1, at rest on the page and pressed`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val page = paletteOf(variant).page

        // When
        val atRest = switchLook(variant, SwitchState(checked = true))
        val pressed = switchLook(variant, SwitchState(checked = true, depth = 1f))

        // Then (WCAG 2.1, 1.4.3: text)
        assertThat(listOf(atRest.label, atRest.subLine)).allSatisfy { ink ->
            assertThat(contrast(ink, page)).isGreaterThanOrEqualTo(4.5)
        }
        assertThat(listOf(pressed.label, pressed.subLine)).allSatisfy { ink ->
            assertThat(contrast(ink, pressed.rowFill)).isGreaterThanOrEqualTo(4.5)
        }
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `a disabled switch is flat, its track in panel inset framed in panel border, its thumb and label in label`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val palette = paletteOf(variant)

        // When pressed, on and off
        val looks = listOf(true, false).map { checked ->
            switchLook(variant, SwitchState(checked = checked, enabled = false, depth = 1f))
        }

        // Then its row does not fill, and its thumb does not grow; on, it still says so by its thumb
        assertThat(looks).allSatisfy { look ->
            assertThat(look.rowFill).isEqualTo(Color.Transparent)
            assertThat(look.track).isEqualTo(palette.panelInset)
            assertThat(look.trackFrame).isEqualTo(palette.panelBorder)
            assertThat(look.trackGloss).isEqualTo(Gloss.None)
            assertThat(look.thumb).isEqualTo(palette.label)
            assertThat(look.label).isEqualTo(palette.label)
            assertThat(look.thumbSize.width).isEqualTo(look.thumbSize.height)
        }
        val (on, off) = looks
        assertThat(on.thumbAtEnd).isTrue()
        // Its check in the colour of the track
        assertThat(on.check).isEqualTo(palette.panelInset)
        assertThat(off.thumbAtEnd).isFalse()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class, names = [COLORFUL_LIGHT, COLORFUL_DARK])
    fun `a switch on bears the gloss of a button on its track, a switch off none`(variant: BloxThemeVariant) {
        // When
        val on = switchLook(variant, SwitchState(checked = true))
        val off = switchLook(variant, SwitchState(checked = false))

        // Then
        assertThat(on.trackGloss).isEqualTo(glossesOf(variant).button)
        assertThat(off.trackGloss).isEqualTo(Gloss.None)
    }

    private companion object {
        const val COLORFUL_LIGHT = "COLORFUL_LIGHT"
        const val COLORFUL_DARK = "COLORFUL_DARK"
    }
}
