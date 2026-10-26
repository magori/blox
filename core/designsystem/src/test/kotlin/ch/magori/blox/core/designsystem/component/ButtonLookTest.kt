package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxButtonColorSets
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxGlossSets
import ch.magori.blox.core.designsystem.theme.BloxGlosses
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.buttonColorsOf
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

internal class ButtonLookTest {
    @Test
    fun `a pressed button goes 2 dp down and takes its pressed fill at once`() {
        // Given a secondary button of the day
        val colors = BloxButtonColorSets.ColorfulLight
        val glosses = BloxGlossSets.ColorfulLight

        // When it is pressed, before a single tick
        val look =
            buttonLook(
                ButtonFamily.SECONDARY,
                colors,
                glosses,
                ButtonState(depth = pressDepth(pressed = true, ticksSinceRelease = 0)),
            )

        // Then
        assertThat(look.drop).isEqualTo(2.dp)
        assertThat(look.fill).isEqualTo(colors.secondary.pressedFill)
        assertThat(look.gloss).isEqualTo(glosses.buttonPressed)
    }

    @Test
    fun `a released button comes back in 6 ticks`() {
        // When a pressed button is released (motion-press, 100 ms: 6 ticks of 1/60 s)
        val depths = (0..7).map { ticks -> pressDepth(pressed = false, ticksSinceRelease = ticks) }

        // Then it comes up a little at each tick, still down at the fifth, back at rest at the sixth
        assertThat(depths.take(6)).isSortedAccordingTo(reverseOrder()).allSatisfy { assertThat(it).isPositive() }
        assertThat(depths.drop(6)).containsOnly(0f)
        assertThat(depths.first()).isEqualTo(1f)
    }

    @Test
    fun `halfway back a button is 1 dp down, still in its pressed fill and ink`() {
        // Given a main button of the night, released 3 ticks ago
        val colors = BloxButtonColorSets.ColorfulDark
        val state = ButtonState(depth = pressDepth(pressed = false, ticksSinceRelease = 3))

        // When
        val look = buttonLook(ButtonFamily.PRIMARY, colors, BloxGlossSets.ColorfulDark, state)

        // Then
        assertThat(look.drop).isEqualTo(1.dp)
        // And its colours do not cross: they change at once, back at rest only
        assertThat(look.fill).isEqualTo(colors.primary.pressedFill)
        assertThat(look.ink).isEqualTo(colors.primary.pressedInk)
    }

    @Test
    fun `in reduced effects a pressed button changes its fill only`() {
        // Given a main button of the day, in reduced effects
        val colors = BloxButtonColorSets.ColorfulLight
        val reduced = BloxEffects(reduced = true, forcedBySystem = false)

        // When it is pressed, then released
        val pressed = buttonLook(
            ButtonFamily.PRIMARY,
            colors,
            BloxGlossSets.ColorfulLight,
            ButtonState(depth = pressDepth(pressed = true, ticksSinceRelease = 0, pressReturnTicks(reduced))),
            reduced,
        )
        val released = pressDepth(pressed = false, ticksSinceRelease = 0, pressReturnTicks(reduced))

        // Then it takes its pressed fill without going down, and is back at rest at once, with no way back up
        assertThat(pressed.fill).isEqualTo(colors.primary.pressedFill)
        assertThat(pressed.drop).isEqualTo(0.dp)
        assertThat(released).isZero()
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndFamilies")
    fun `a disabled button has no gloss, its label in label and its frame in panel border`(
        theme: String,
        family: ButtonFamily,
        variant: Variant,
    ) {
        // When, even pressed
        val look = buttonLook(family, variant.colors, variant.glosses, ButtonState(enabled = false, depth = 1f))

        // Then it does not stand out: flat, in place, its fill the inset of the panels
        assertThat(look.gloss).isEqualTo(Gloss.None)
        assertThat(look.ink).isEqualTo(variant.palette.label)
        assertThat(look.frame).isEqualTo(variant.palette.panelBorder)
        assertThat(look.fill).isEqualTo(variant.palette.panelInset)
        assertThat(look.drop).isEqualTo(0.dp)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndFamilies")
    fun `the label of each family stands out from its fill by 4,5 to 1 at least, at rest, pressed and coming back`(
        theme: String,
        family: ButtonFamily,
        variant: Variant,
    ) {
        // When at rest, pressed, and at each tick of its way back up
        val depths = listOf(0f, 1f) + (0..BloxMotion.press).map { pressDepth(pressed = false, ticksSinceRelease = it) }
        val looks = depths.map { depth ->
            buttonLook(family, variant.colors, variant.glosses, ButtonState(depth = depth))
        }

        // Then (WCAG 2.1, 1.4.3: the labels of 15 and 16 px are not large text)
        assertThat(looks).allSatisfy { look -> assertThat(contrast(look.ink, look.fill)).isGreaterThanOrEqualTo(4.5) }
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndFamilies")
    fun `a focused button bears a 3 dp outline in ink`(
        theme: String,
        family: ButtonFamily,
        variant: Variant,
    ) {
        // When
        val focused = buttonLook(family, variant.colors, variant.glosses, ButtonState(focused = true))
        val notFocused = buttonLook(family, variant.colors, variant.glosses, ButtonState())

        // Then a ring of 3 dp in ink, 3 dp off its edge, for the keyboard only
        assertThat(focused.focusRing).isEqualTo(FocusRing(color = variant.palette.ink, width = 3.dp, gap = 3.dp))
        assertThat(notFocused.focusRing).isNull()
    }

    @ParameterizedTest
    @EnumSource(ButtonFamily::class)
    fun `every family is 48 dp high at least, Play 72 and a main or secondary button 56`(family: ButtonFamily) {
        // Then (touch-target; 48 dp wide at least on the screen, BubbleButtonTest): an icon a disc of 48, a danger
        // button as high as a secondary one
        assertThat(family.minHeight).isGreaterThanOrEqualTo(BloxSizes.minTouchTarget)
        assertThat(ButtonFamily.entries.map { it.minHeight }).containsExactly(72.dp, 56.dp, 56.dp, 56.dp, 48.dp)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a danger button is framed and labelled in danger at rest, filled in danger pressed`(
        theme: String,
        variant: Variant,
    ) {
        // When
        val atRest = buttonLook(ButtonFamily.DANGER, variant.colors, variant.glosses, ButtonState())
        val pressed = buttonLook(ButtonFamily.DANGER, variant.colors, variant.glosses, ButtonState(depth = 1f))

        // Then at rest, filled as a secondary button (docs/design-system/README.md, Paramètres: Effacer, « cadre et
        // texte danger »); pressed, as the answer of a window that deletes: on the pressed fill of a secondary button,
        // the danger would not stand out by 4,5 to 1
        assertThat(atRest.fill).isEqualTo(variant.colors.secondary.fill)
        assertThat(atRest.ink).isEqualTo(variant.palette.danger)
        assertThat(pressed.fill).isEqualTo(variant.palette.danger)
        assertThat(pressed.ink).isEqualTo(variant.palette.dangerInk)
        assertThat(listOf(atRest, pressed)).allSatisfy { look ->
            assertThat(look.frame).isEqualTo(variant.palette.danger)
        }
    }

    @Test
    fun `each variant draws its buttons in its own colours`() {
        // Then
        assertThat(BloxThemeVariant.entries.associateWith(::buttonColorsOf)).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                BloxThemeVariant.ORIGINAL to BloxButtonColorSets.Original,
                BloxThemeVariant.COLORFUL_LIGHT to BloxButtonColorSets.ColorfulLight,
                BloxThemeVariant.COLORFUL_DARK to BloxButtonColorSets.ColorfulDark,
            ),
        )
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndFamilies")
    fun `the frame of a secondary or an icon button stands out from the page by 3 to 1 at least`(
        theme: String,
        family: ButtonFamily,
        variant: Variant,
    ) {
        // When
        val frame = buttonLook(family, variant.colors, variant.glosses, ButtonState()).frame

        // Then (WCAG 2.1, 1.4.11): a main button stands out by its fill and its label, it has no frame
        if (family.isPrimary) {
            assertThat(frame).isNull()
        } else {
            assertThat(contrast(frame!!, variant.palette.page)).isGreaterThanOrEqualTo(3.0)
        }
    }

    /** A variant of a theme: its palette, the colours of its buttons and its reliefs. */
    internal class Variant(val palette: BloxPalette, val colors: BloxButtonColors, val glosses: BloxGlosses)

    companion object {
        private val VARIANTS = listOf(
            "Original" to Variant(BloxPalettes.Original, BloxButtonColorSets.Original, BloxGlossSets.Original),
            "Couleurs, jour" to
                Variant(BloxPalettes.ColorfulLight, BloxButtonColorSets.ColorfulLight, BloxGlossSets.ColorfulLight),
            "Couleurs, nuit" to
                Variant(BloxPalettes.ColorfulDark, BloxButtonColorSets.ColorfulDark, BloxGlossSets.ColorfulDark),
        )

        @JvmStatic
        fun variants(): List<Arguments> = VARIANTS.map { (theme, variant) -> Arguments.of(theme, variant) }

        @JvmStatic
        fun variantsAndFamilies(): List<Arguments> = VARIANTS.flatMap { (theme, variant) ->
            ButtonFamily.entries.map { family -> Arguments.of(theme, family, variant) }
        }
    }
}
