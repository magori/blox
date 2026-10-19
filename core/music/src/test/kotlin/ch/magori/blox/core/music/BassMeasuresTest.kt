package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class BassMeasuresTest {
    @Test
    fun `the share of smooth changes counts the changes of chord the bass makes by a fourth at most, loop included`() {
        // Given C E under C, a semitone up to F under F, a minor seventh down to G under G, and a fourth up back to C
        val bass = bassOf("C3 2, E3 2", "F3 4", "G2 4")

        // When
        val share = BassMeasures(formIn(C_MAJOR, "C", "F", "G"), bass).smoothShare

        // Then
        assertThat(share).isCloseTo(2.0 / 3, within(1e-9))
    }

    @Test
    fun `a bass under a single chord makes no change to leap at, so all of its changes are smooth`() {
        // Given a root and a fifth under C major all along
        val bass = bassOf("C3 2, G2 2")

        // When
        val share = BassMeasures(formIn(C_MAJOR, "C"), bass).smoothShare

        // Then
        assertThat(share).isEqualTo(1.0)
    }
}
