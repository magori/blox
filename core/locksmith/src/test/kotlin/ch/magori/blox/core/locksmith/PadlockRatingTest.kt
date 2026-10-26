package ch.magori.blox.core.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PadlockRatingTest {
    @Test
    fun `the rating is 10 per edge of the outline, 30 per kind of piece, minus 3 per cell of the box`() {
        // Given: an outline of 18 edges, 2 kinds of pieces (I, O), a box of 5 × 4 cells
        val padlock = Padlock.parse(
            """
            ##########
            #1111#####
            ##2222####
            ####33####
            ####33####
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        // When
        val rating = PadlockGenerator.rating(padlock)

        // Then
        assertThat(rating).isEqualTo(10 * 18 + 30 * 2 - 3 * 20)
    }

    @Test
    fun `a rectangular hole is ruled out`() {
        // Given: three flat I fill a box of 4 × 3 cells
        val padlock = Padlock.parse(
            """
            ##########
            #1111#####
            #2222#####
            #3333#####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        // When
        val rating = PadlockGenerator.rating(padlock)

        // Then
        assertThat(rating).isNull()
    }
}
