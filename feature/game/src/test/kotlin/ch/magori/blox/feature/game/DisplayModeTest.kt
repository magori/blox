package ch.magori.blox.feature.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DisplayModeTest {
    @Test
    fun `on a 120 Hz screen the game asks for the 60 Hz mode`() {
        // Given a screen at 120 Hz, which can also run at 60 Hz
        val at120 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 120f)
        val at60 = DisplayMode(id = 2, width = 1080, height = 2400, refreshRate = 60f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, at60))

        // Then
        assertThat(chosen).isEqualTo(at60)
    }

    @Test
    fun `the game never asks for a mode of another resolution`() {
        // Given a screen at 120 Hz whose 60 Hz modes are at its resolution and at a lower one
        val at120 = DisplayMode(id = 1, width = 1440, height = 3200, refreshRate = 120f)
        val lowerAt60 = DisplayMode(id = 2, width = 1080, height = 2400, refreshRate = 60f)
        val at60 = DisplayMode(id = 3, width = 1440, height = 3200, refreshRate = 60f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, lowerAt60, at60))

        // Then
        assertThat(chosen).isEqualTo(at60)
    }

    @Test
    fun `the game never asks for a mode of the same width but another height`() {
        // Given a screen at 120 Hz with a 60 Hz mode of its width but another height, listed first
        val at120 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 120f)
        val shorterAt60 = DisplayMode(id = 2, width = 1080, height = 1920, refreshRate = 60f)
        val at60 = DisplayMode(id = 3, width = 1080, height = 2400, refreshRate = 60f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, shorterAt60, at60))

        // Then
        assertThat(chosen).isEqualTo(at60)
    }

    @Test
    fun `the game never asks for a mode of the same height but another width`() {
        // Given a screen at 120 Hz with a 60 Hz mode of its height but another width, listed first
        val at120 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 120f)
        val narrowerAt60 = DisplayMode(id = 2, width = 720, height = 2400, refreshRate = 60f)
        val at60 = DisplayMode(id = 3, width = 1080, height = 2400, refreshRate = 60f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, narrowerAt60, at60))

        // Then
        assertThat(chosen).isEqualTo(at60)
    }

    @Test
    fun `a mode slower than 60 Hz is never asked for, a tick would then miss its frame`() {
        // Given a screen at 120 Hz that can also run at 60 and 30 Hz
        val at120 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 120f)
        val at30 = DisplayMode(id = 2, width = 1080, height = 2400, refreshRate = 30f)
        val at60 = DisplayMode(id = 3, width = 1080, height = 2400, refreshRate = 60f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, at30, at60))

        // Then
        assertThat(chosen).isEqualTo(at60)
    }

    @Test
    fun `a mode the screen reports a hair under 60 Hz counts as 60 Hz`() {
        // Given a screen at 120 Hz whose 60 Hz mode is reported at 59.94 Hz
        val at120 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 120f)
        val at59 = DisplayMode(id = 2, width = 1080, height = 2400, refreshRate = 59.94f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, at59))

        // Then
        assertThat(chosen).isEqualTo(at59)
    }

    @Test
    fun `between a mode at 59,94 Hz and one at 60 Hz the game asks for 60 Hz`() {
        // Given a screen at 120 Hz that can also run at 59.94 and 60 Hz
        val at120 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 120f)
        val at59 = DisplayMode(id = 2, width = 1080, height = 2400, refreshRate = 59.94f)
        val at60 = DisplayMode(id = 3, width = 1080, height = 2400, refreshRate = 60f)

        // When
        val chosen = gameDisplayMode(current = at120, supported = listOf(at120, at59, at60))

        // Then
        assertThat(chosen).isEqualTo(at60)
    }

    @Test
    fun `without a 60 Hz mode the game asks for 120 Hz rather than 90 Hz, two frames for each tick`() {
        // Given a screen that runs at 90 or 120 Hz only
        val at90 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 90f)
        val at120 = DisplayMode(id = 2, width = 1080, height = 2400, refreshRate = 120f)

        // When
        val chosen = gameDisplayMode(current = at90, supported = listOf(at90, at120))

        // Then
        assertThat(chosen).isEqualTo(at120)
    }

    @Test
    fun `a screen with no mode at a multiple of 60 Hz is asked for nothing`() {
        // Given a screen that runs at 90 Hz only
        val at90 = DisplayMode(id = 1, width = 1080, height = 2400, refreshRate = 90f)

        // When
        val chosen = gameDisplayMode(current = at90, supported = listOf(at90))

        // Then
        assertThat(chosen).isNull()
    }
}
