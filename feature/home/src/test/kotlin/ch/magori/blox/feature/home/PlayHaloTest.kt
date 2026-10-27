package ch.magori.blox.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlayHaloTest {
    @Test
    fun `the halo of Jouer stays off for 3 seconds`() {
        // When the home has shown for less than 3 seconds, 180 ticks
        val halos = (0 until THREE_SECONDS).map(::playHalo)

        // Then no halo
        assertThat(halos).containsOnly(0f)
    }

    @Test
    fun `the halo of Jouer pulses 3 times after 3 seconds, then once every 10 seconds after the third`() {
        // When the home has shown for 26 seconds
        val halos = (0 until 26 * SECOND).map(::playHalo)

        // Then three pulses of a second from 3 seconds, then, 10 seconds after the third, one at 15 and at 25 seconds
        val starts = halos.indices.filter { tick ->
            halos[tick] > 0f && (tick == 0 || halos[tick - 1] == 0f || halos[tick - 1] >= halos[tick])
        }
        assertThat(starts).containsExactly(3 * SECOND, 4 * SECOND, 5 * SECOND, 15 * SECOND, 25 * SECOND)
        assertThat(halos).allMatch { it in 0f..1f }
    }

    @Test
    fun `the halo is a pill around Jouer, which grows to its full spread as it fades`() {
        // Given Jouer, 300 × 72 px, and a halo spreading 12 px at most
        val button = Size(300f, 72f)

        // When the pulse is half gone
        val ring = haloRing(progress = 0.5f, button = button, spread = 12f)

        // Then a pill 6 px around it, half faded
        assertThat(ring).isEqualTo(
            HaloRing(topLeft = Offset(-6f, -6f), size = Size(312f, 84f), corner = 42f, alpha = 0.5f),
        )
    }

    @Test
    fun `without a pulse, there is no halo to draw`() {
        assertThat(haloRing(progress = 0f, button = Size(300f, 72f), spread = 12f)).isNull()
    }
}

private const val SECOND = 60
private const val THREE_SECONDS = 3 * SECOND
