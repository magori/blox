package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The curves the effects follow, tick by tick (ADR 0024), shared by the Locksmith and the musical garden. */
class EffectCurvesTest {
    @Test
    fun `a fade goes from 1 at its first tick down to 0 at its length, nothing before nor after it`() {
        // When
        val fades = (-1..9).map { tick -> fadeOut(tick, length = 8) }

        // Then
        assertThat(fades.first()).isZero()
        assertThat(fades[1]).isEqualTo(1f)
        assertThat(fades.subList(1, 10)).isSortedAccordingTo(reverseOrder())
        assertThat(fades[5]).isEqualTo(0.5f)
        assertThat(fades.subList(9, 11)).containsOnly(0f)
    }

    @Test
    fun `a swell goes from 0 at its first tick up to 1 halfway, back down to 0 at its length, nothing outside it`() {
        // When
        val swells = (-1..7).map { tick -> swell(tick, length = 6) }

        // Then
        assertThat(swells.first()).isZero()
        assertThat(swells[1]).isZero()
        assertThat(swells.subList(1, 5)).isSorted()
        assertThat(swells[4]).isEqualTo(1f)
        assertThat(swells.subList(4, 8)).isSortedAccordingTo(reverseOrder())
        assertThat(swells.subList(7, 9)).containsOnly(0f)
    }
}
