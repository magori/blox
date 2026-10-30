package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PadlockSolverTest {
    @Test
    fun `a hole that only its solution fills has one solution`() {
        // Given: the O, the I and the T can only lie where the solution puts them
        val padlock = Padlock.parse(
            """
            ##########
            #112222###
            #11333####
            ####3#####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )

        // When
        val solutions = PadlockSolver.count(padlock.hole, padlock.givenPieces, cap = 5)

        // Then
        assertThat(solutions).isEqualTo(1)
    }

    @Test
    fun `every cover is counted once, pieces of a type being alike`() {
        // Given: four I fill a square of 4 × 4 lying flat or standing, in any order
        val padlock = Padlock.parse(SQUARE_OF_IS)

        // When
        val solutions = PadlockSolver.count(padlock.hole, padlock.givenPieces, cap = 5)

        // Then
        assertThat(solutions).isEqualTo(2)
    }

    @Test
    fun `the count stops at the cap`() {
        // Given: two covers of the square
        val padlock = Padlock.parse(SQUARE_OF_IS)

        // When
        val solutions = PadlockSolver.count(padlock.hole, padlock.givenPieces, cap = 1)

        // Then
        assertThat(solutions).isEqualTo(1)
    }

    @Test
    fun `pieces that cannot fill the hole have no solution`() {
        // Given: four S cannot fill a square
        val padlock = Padlock.parse(SQUARE_OF_IS)

        // When
        val solutions = PadlockSolver.count(padlock.hole, List(4) { PieceType.S }, cap = 5)

        // Then
        assertThat(solutions).isZero
    }

    private companion object {
        val SQUARE_OF_IS =
            """
            ##########
            #1111#####
            #2222#####
            #3333#####
            #4444#####
            ##########
            ##########
            ##########
            ##########
            ##########
            """
    }
}
