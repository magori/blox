package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SheetEntranceTest {
    @Test
    fun `a sheet rises in 18 ticks, and in reduced effects fades in within 8 ticks`() {
        // Given the full effects, then reduced ones
        val reduced = BloxEffects(reduced = true, forcedBySystem = false)

        // When the sheet comes in, tick by tick
        val rising = (0..RISE_TICKS).map { sheetEntrance(it, BloxEffects.Full) }
        val fading = (0..RISE_TICKS).map { sheetEntrance(it, reduced) }

        // Then it starts below and transparent, then rises and shows, settled at its 18th tick and not before
        assertThat(rising.first().rise).isEqualTo(1f)
        assertThat(rising.first().panelAlpha).isZero()
        assertThat(rising.zipWithNext()).allSatisfy { (before, after) ->
            assertThat(after.rise).isLessThanOrEqualTo(before.rise)
            assertThat(after.panelAlpha).isGreaterThanOrEqualTo(before.panelAlpha)
        }
        assertThat(rising[RISE_TICKS - 1]).isNotEqualTo(SheetEntrance.Settled)
        assertThat(rising[RISE_TICKS]).isEqualTo(SheetEntrance.Settled)

        // And in reduced effects it does not move: it fades in, settled at its 8th tick and not before
        assertThat(fading).allSatisfy { assertThat(it.rise).isZero() }
        assertThat(fading.first().panelAlpha).isZero()
        assertThat(fading[FADE_TICKS - 1]).isNotEqualTo(SheetEntrance.Settled)
        assertThat(fading[FADE_TICKS]).isEqualTo(SheetEntrance.Settled)
    }

    @Test
    fun `the scrim of a sheet fades in within 8 ticks, the effects reduced or not`() {
        // Given the full effects, then reduced ones
        listOf(BloxEffects.Full, BloxEffects(reduced = true, forcedBySystem = false)).forEach { effects ->
            // When the sheet comes in
            val scrim = (0..RISE_TICKS).map { sheetEntrance(it, effects).scrimAlpha }

            // Then its scrim goes from nothing to whole in motion reduced fade, then stays
            assertThat(scrim.first()).isZero()
            assertThat(scrim[FADE_TICKS - 1]).isLessThan(1f)
            assertThat(scrim.drop(FADE_TICKS)).containsOnly(1f)
        }
    }

    private companion object {
        /** `motion-card`, then `motion-reduced-fade`, in ticks. */
        const val RISE_TICKS = 18
        const val FADE_TICKS = 8
    }
}
