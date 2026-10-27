package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.graphics.Color
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
        val state = ButtonState(depth = pressDepth(pressed = true, ticksSinceRelease = 0))
        val look = buttonLook(ButtonFamily.SECONDARY, colors, glosses, state)

        // Then
        assertThat(buttonDrop(state.enabled, state.depth, BloxEffects.Full)).isEqualTo(2.dp)
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
        assertThat(buttonDrop(state.enabled, state.depth, BloxEffects.Full)).isEqualTo(1.dp)
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
        val state = ButtonState(depth = pressDepth(pressed = true, ticksSinceRelease = 0, pressReturnTicks(reduced)))
        val pressed = buttonLook(ButtonFamily.PRIMARY, colors, BloxGlossSets.ColorfulLight, state)
        val released = pressDepth(pressed = false, ticksSinceRelease = 0, pressReturnTicks(reduced))

        // Then it takes its pressed fill without going down, and is back at rest at once, with no way back up
        assertThat(pressed.fill).isEqualTo(colors.primary.pressedFill)
        assertThat(buttonDrop(state.enabled, state.depth, reduced)).isEqualTo(0.dp)
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
        val state = ButtonState(enabled = false, depth = 1f)
        val look = buttonLook(family, variant.colors, variant.glosses, state)

        // Then it does not stand out: flat, in place, its fill the inset of the panels
        assertThat(look.gloss).isEqualTo(Gloss.None)
        assertThat(look.ink).isEqualTo(variant.palette.label)
        assertThat(look.frame).isEqualTo(variant.palette.panelBorder)
        assertThat(look.fill).isEqualTo(variant.palette.panelInset)
        assertThat(buttonDrop(state.enabled, state.depth, BloxEffects.Full)).isEqualTo(0.dp)
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

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndFamilies")
    fun `a selected button is filled in accent, framed and pressed in accent pressed, its ink in accent ink`(
        theme: String,
        family: ButtonFamily,
        variant: Variant,
    ) {
        // When at rest and pressed
        val atRest = variant.look(family, SELECTED)
        val pressed = variant.look(family, SELECTED.copy(depth = 1f))

        // Then
        val palette = variant.palette
        assertThat(listOf(atRest.fill, atRest.frame, atRest.ink))
            .containsExactly(palette.accent, palette.accentPressed, palette.accentInk)
        assertThat(listOf(pressed.fill, pressed.frame, pressed.ink))
            .containsExactly(palette.accentPressed, palette.accentPressed, palette.accentInk)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `the frame of a selected button stands out from the page by 3 to 1 at least`(
        theme: String,
        variant: Variant,
    ) {
        // When
        val frame = variant.look(ButtonFamily.SECONDARY, SELECTED).frame

        // Then (WCAG 2.1, 1.4.11): by day, the accent itself is only 2,3 to 1 on the page
        assertThat(contrast(frame!!, variant.palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a disabled selected button stays in accent, flat and in place`(
        theme: String,
        variant: Variant,
    ) {
        // When, even pressed
        val state = SELECTED.copy(enabled = false, depth = 1f)
        val look = variant.look(ButtonFamily.SECONDARY, state)

        // Then the choice made still shows, without relief
        val palette = variant.palette
        assertThat(listOf(look.fill, look.frame, look.ink))
            .containsExactly(palette.accent, palette.accentPressed, palette.accentInk)
        assertThat(look.gloss).isEqualTo(Gloss.None)
        assertThat(buttonDrop(state.enabled, state.depth, BloxEffects.Full)).isEqualTo(0.dp)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `the ink of a selected button stands out from its fill by 4,5 to 1 at least, at rest and pressed`(
        theme: String,
        variant: Variant,
    ) {
        // When (its colours are the same in every family)
        val looks = listOf(0f, 1f).map { depth -> variant.look(ButtonFamily.SECONDARY, SELECTED.copy(depth = depth)) }

        // Then (WCAG 2.1, 1.4.3)
        assertThat(looks).allSatisfy { look -> assertThat(contrast(look.ink, look.fill)).isGreaterThanOrEqualTo(4.5) }
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("variantsAndFamilies")
    fun `an active button keeps the pressed fill at rest, without going down`(
        theme: String,
        family: ButtonFamily,
        variant: Variant,
    ) {
        // Given the same button pressed
        val pressed = variant.look(family, ButtonState(depth = 1f))

        // When it is active, at rest (Écouter while it plays)
        val state = ButtonState(active = true)
        val active = variant.look(family, state)

        // Then it looks pressed, in place
        assertThat(active).isEqualTo(pressed)
        assertThat(buttonDrop(state.enabled, state.depth, BloxEffects.Full)).isEqualTo(0.dp)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a bare icon button has no fill, no frame and no gloss at rest, and takes button pressed when pressed`(
        theme: String,
        variant: Variant,
    ) {
        // When at rest and pressed
        val atRest = variant.look(ButtonFamily.ICON, BARE)
        val pressed = variant.look(ButtonFamily.ICON, BARE.copy(depth = 1f))

        // Then nothing but its glyph at rest
        assertThat(atRest.fill).isEqualTo(Color.Transparent)
        assertThat(atRest.frame).isNull()
        assertThat(atRest.gloss).isEqualTo(Gloss.None)
        // And a disc of button pressed when pressed
        assertThat(pressed.fill).isEqualTo(variant.palette.buttonPressed)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a disabled bare icon button stays bare, its glyph in label`(
        theme: String,
        variant: Variant,
    ) {
        // When, even pressed (− of the tempo at its slowest)
        val state = BARE.copy(enabled = false, depth = 1f)
        val look = variant.look(ButtonFamily.ICON, state)

        // Then
        assertThat(look.fill).isEqualTo(Color.Transparent)
        assertThat(look.frame).isNull()
        assertThat(look.ink).isEqualTo(variant.palette.label)
        assertThat(look.gloss).isEqualTo(Gloss.None)
        assertThat(buttonDrop(state.enabled, state.depth, BloxEffects.Full)).isEqualTo(0.dp)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `the glyph of a bare button stands out from the page by 3 to 1 at least`(
        theme: String,
        variant: Variant,
    ) {
        // When
        val ink = variant.look(ButtonFamily.ICON, BARE).ink

        // Then (WCAG 2.1, 1.4.11)
        assertThat(contrast(ink, variant.palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `the glyph of a pressed bare button stands out from button pressed by 4,5 to 1 at least`(
        theme: String,
        variant: Variant,
    ) {
        // When
        val look = variant.look(ButtonFamily.ICON, BARE.copy(depth = 1f))

        // Then
        assertThat(contrast(look.ink, look.fill)).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a pressed bare button is framed in button outline, which stands out from the page by 3 to 1 at least`(
        theme: String,
        variant: Variant,
    ) {
        // When
        val frame = variant.look(ButtonFamily.ICON, BARE.copy(depth = 1f)).frame

        // Then (by day, button pressed is only 1,4 to 1 on the page)
        assertThat(frame).isEqualTo(variant.palette.buttonOutline)
        assertThat(contrast(frame!!, variant.palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("colorfulVariants")
    fun `a bare button on ink has its glyph in page at rest, and takes button pressed when pressed, both readable`(
        theme: String,
        variant: Variant,
    ) {
        // When at rest and pressed, on the ink of a message
        val atRest = variant.look(ButtonFamily.ICON, BARE_ON_INK)
        val pressed = variant.look(ButtonFamily.ICON, BARE_ON_INK.copy(depth = 1f))

        // Then its glyph in page on the ink at rest
        assertThat(atRest.fill).isEqualTo(Color.Transparent)
        assertThat(atRest.ink).isEqualTo(variant.palette.page)
        assertThat(contrast(atRest.ink, variant.palette.ink)).isGreaterThanOrEqualTo(4.5)
        // And a disc of button pressed when pressed, its glyph standing out
        assertThat(pressed.fill).isEqualTo(variant.palette.buttonPressed)
        assertThat(contrast(pressed.ink, pressed.fill)).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest
    @EnumSource(ButtonFamily::class)
    fun `only a main or a secondary button may be compact, 48 dp high`(family: ButtonFamily) {
        // When
        val height = runCatching { family.minHeight(ButtonSize.COMPACT) }

        // Then
        if (family == ButtonFamily.PRIMARY || family == ButtonFamily.SECONDARY) {
            assertThat(height.getOrThrow()).isEqualTo(48.dp)
        } else {
            assertThat(height.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    /** A variant of a theme: its palette, the colours of its buttons and its reliefs. */
    internal class Variant(val palette: BloxPalette, val colors: BloxButtonColors, val glosses: BloxGlosses) {
        /** How a button of [family] in [state] is drawn in this variant. */
        fun look(
            family: ButtonFamily,
            state: ButtonState,
        ) = buttonLook(family, colors, glosses, state)
    }

    companion object {
        /** A selected button, and a bare one, at rest. */
        private val SELECTED = ButtonState(emphasis = ButtonEmphasis.SELECTED)
        private val BARE = ButtonState(emphasis = ButtonEmphasis.BARE)
        private val BARE_ON_INK = ButtonState(emphasis = ButtonEmphasis.BARE, onInk = true)

        private val VARIANTS = listOf(
            "Original" to Variant(BloxPalettes.Original, BloxButtonColorSets.Original, BloxGlossSets.Original),
            "Couleurs, jour" to
                Variant(BloxPalettes.ColorfulLight, BloxButtonColorSets.ColorfulLight, BloxGlossSets.ColorfulLight),
            "Couleurs, nuit" to
                Variant(BloxPalettes.ColorfulDark, BloxButtonColorSets.ColorfulDark, BloxGlossSets.ColorfulDark),
        )

        @JvmStatic
        fun variants(): List<Arguments> = VARIANTS.map { (theme, variant) -> Arguments.of(theme, variant) }

        /** The variants of the Colorful theme, day and night. */
        @JvmStatic
        fun colorfulVariants(): List<Arguments> = VARIANTS.filter { (theme, _) -> theme.startsWith("Couleurs") }
            .map { (theme, variant) -> Arguments.of(theme, variant) }

        @JvmStatic
        fun variantsAndFamilies(): List<Arguments> = VARIANTS.flatMap { (theme, variant) ->
            ButtonFamily.entries.map { family -> Arguments.of(theme, family, variant) }
        }
    }
}
