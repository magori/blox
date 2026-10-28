package ch.magori.blox.core.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

class PadlockRatingTest {
    @Test
    fun `the rating is 10 per edge of the outline, 30 per kind of piece, minus 3 per cell of the box`() {
        // Given: an outline of 18 edges, 2 kinds of pieces (I, O), a box of 5 × 4 cells
        val padlock = Padlock.parse(STAIRCASE)

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

    @Test
    fun `each edge of the outline weighs as its shape asks, each solution takes off the weight of its preference`() {
        // Given: an outline of 18 edges, 2 kinds of pieces (I, O), a box of 5 × 4 cells, 1 solution
        val padlock = Padlock.parse(STAIRCASE)

        // When
        val rating = PadlockGenerator.rating(padlock, HoleShape.COMPACT, SolutionPreference.FEW)

        // Then
        assertThat(rating).isEqualTo(
            HoleShape.COMPACT.outlineWeight * 18 + 30 * 2 - 3 * 20 - SolutionPreference.FEW.weightPerSolution * 1,
        )
    }

    @Test
    fun `the weights are those calibrated by the measure of padlocks 1 to 100`() {
        // A sinuous hole is rated as before the tiers; mixed and compact ones less by the outline. Each solution
        // takes off 15 when few are preferred, 45 when a unique one is (P7.29, PadlockMeasure).
        assertThat(HoleShape.entries.associateWith { it.outlineWeight })
            .containsExactly(entry(HoleShape.SINUOUS, 10), entry(HoleShape.MIXED, 5), entry(HoleShape.COMPACT, 2))
        assertThat(SolutionPreference.entries.associateWith { it.weightPerSolution }).containsExactly(
            entry(SolutionPreference.FREE, 0),
            entry(SolutionPreference.FEW, 15),
            entry(SolutionPreference.UNIQUE, 45),
        )
    }

    private companion object {
        val STAIRCASE =
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
            """
    }
}
