package ch.magori.blox.feature.garden

import ch.magori.blox.core.designsystem.theme.BloxEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The feedback of a garden kept from « Mes jardins », in ticks from the keeping: the sheet goes down, then the dot of
 * the disc goes out, then the message « « X » gardé. » comes (ADR 0024).
 */
class GardenKeptFeedbackTest {
    @Test
    fun `once a garden is kept, the dot of Mes jardins stays while the sheet goes down, then fades out in 8 ticks`() {
        // When, tick by tick from the keeping
        val dot = (0..SHEET_DOWN + FADE).map { keptDotLight(it, FULL, afterSheet = true) }

        // Then
        assertThat(dot.take(SHEET_DOWN)).containsOnly(1f)
        assertThat(dot.drop(SHEET_DOWN)).isSortedAccordingTo(reverseOrder())
        assertThat(dot[SHEET_DOWN + FADE - 1]).isGreaterThan(0f)
        assertThat(dot.last()).isZero()
        assertThat(keptDotLight(Int.MAX_VALUE, FULL, afterSheet = true)).isZero()
        assertThat(keptDotTicks(FULL)).isEqualTo(SHEET_DOWN + FADE)
    }

    @Test
    fun `the dot going out with no sheet going down, as after Annuler, starts to fade at once, within 8 ticks`() {
        // When, tick by tick from the change that leaves the garden no longer changed, the sheet closed already or open
        val dot = (0..FADE).map { keptDotLight(it, FULL, afterSheet = false) }

        // Then
        assertThat(dot.first()).isEqualTo(1f)
        assertThat(dot[1]).isLessThan(1f)
        assertThat(dot).isSortedAccordingTo(reverseOrder())
        assertThat(dot.last()).isZero()
    }

    @Test
    fun `once the dot is out, the message of the garden kept rises into place and shows in 18 ticks`() {
        // When, tick by tick from the keeping
        val message = (0..SHEET_DOWN + FADE + RISE).map { keptMessageEntrance(it, FULL) }

        // Then out of sight until the dot is out, then rising and showing, in place at the end
        assertThat(message.take(SHEET_DOWN + FADE)).allSatisfy { assertThat(it.alpha).isZero() }
        val rising = message.drop(SHEET_DOWN + FADE)
        assertThat(rising.first().rise).isEqualTo(1f)
        assertThat(rising.map { it.rise }).isSortedAccordingTo(reverseOrder())
        assertThat(rising.map { it.alpha }).isSorted()
        assertThat(rising[RISE - 1]).isNotEqualTo(MessageEntrance.Settled)
        assertThat(rising.last()).isEqualTo(MessageEntrance.Settled)
        assertThat(keptMessageEntrance(Int.MAX_VALUE, FULL)).isEqualTo(MessageEntrance.Settled)
    }

    @Test
    fun `in reduced effects the dot starts to fade at once, within 8 ticks, then the message shows unmoved`() {
        // When, tick by tick from the keeping, the sheet gone at once
        val dot = (0..FADE).map { keptDotLight(it, REDUCED, afterSheet = true) }
        val message = (0..FADE).map { keptMessageEntrance(it, REDUCED) }

        // Then
        assertThat(dot.first()).isEqualTo(1f)
        assertThat(dot).isSortedAccordingTo(reverseOrder())
        assertThat(keptDotTicks(REDUCED)).isEqualTo(FADE)
        assertThat(dot.last()).isZero()
        assertThat(message.take(FADE)).allSatisfy { assertThat(it.alpha).isZero() }
        assertThat(message.last()).isEqualTo(MessageEntrance.Settled)
    }

    private companion object {
        val FULL = BloxEffects.Full
        val REDUCED = BloxEffects(reduced = true, forcedBySystem = false)

        /** `motion-card`, the sheet going down then the message rising; `motion-reduced-fade`, the dot going out. */
        const val SHEET_DOWN = 18
        const val RISE = 18
        const val FADE = 8
    }
}
