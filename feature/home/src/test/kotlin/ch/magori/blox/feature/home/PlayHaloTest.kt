package ch.magori.blox.feature.home

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
    fun `the halo of Jouer pulses 3 times after 3 seconds, then once every 10 seconds`() {
        // When the home has shown for 24 seconds
        val halos = (0 until 24 * SECOND).map(::playHalo)

        // Then three pulses of a second from 3 seconds, then one at 13 and at 23 seconds
        val starts = halos.indices.filter { tick ->
            halos[tick] > 0f && (tick == 0 || halos[tick - 1] == 0f || halos[tick - 1] >= halos[tick])
        }
        assertThat(starts).containsExactly(3 * SECOND, 4 * SECOND, 5 * SECOND, 13 * SECOND, 23 * SECOND)
        assertThat(halos).allMatch { it in 0f..1f }
    }
}

private const val SECOND = 60
private const val THREE_SECONDS = 3 * SECOND
