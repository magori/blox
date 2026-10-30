package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.StableRandom
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GardenComposerTest {
    @Test
    fun `the same mood and seed give the same garden`() {
        // When
        val first = GardenComposer.compose(GardenMood.JOYFUL, seed = 20261028)
        val second = GardenComposer.compose(GardenMood.JOYFUL, seed = 20261028)

        // Then
        assertThat(first).isEqualTo(second)
    }

    @ParameterizedTest
    @EnumSource(GardenMood::class)
    fun `the composed garden is in the mood asked`(mood: GardenMood) {
        // When
        val garden = GardenComposer.compose(mood, seed = 20261028)

        // Then
        assertThat(garden.mood).isEqualTo(mood)
    }

    @Test
    fun `the best candidate is kept, the first one among equals`() {
        // Given: candidates rated 10, 30, 30 and 20, then candidates given up
        val gardens = (0..3).map { column -> gardenWithAnO(column) }
        val points = gardens.zip(listOf(10, 30, 30, 20)).toMap()
        val script = (gardens + List(16) { null }).iterator()

        // When
        val garden = GardenComposer.choose(
            GardenMood.CALM,
            seed = 1,
            compose = { _, _ -> script.next() },
            points = points::getValue,
        )

        // Then
        assertThat(garden).isEqualTo(gardens[1])
    }

    @Test
    fun `the composed garden is the best rated of its candidates`() {
        // Given: the candidates of the seed, recorded as they are composed
        val candidates = mutableListOf<Garden>()
        GardenComposer.choose(GardenMood.MYSTERIOUS, seed = 1, compose = { mood, random ->
            GardenComposer.candidate(mood, random)?.also(candidates::add)
        })

        // When
        val garden = GardenComposer.compose(GardenMood.MYSTERIOUS, seed = 1)

        // Then: seed 1 is chosen so that the best one is not the first one
        val best = candidates.maxBy { GardenRating.of(it).points }
        assertThat(candidates).hasSize(20)
        assertThat(candidates.first()).isNotEqualTo(best)
        assertThat(garden).isEqualTo(best)
    }

    @Test
    fun `different seeds give different gardens`() {
        assertThat(GardenComposer.compose(GardenMood.CALM, seed = 1))
            .isNotEqualTo(GardenComposer.compose(GardenMood.CALM, seed = 2))
    }

    @Test
    fun `a candidate does not depend on the draws of the previous ones`() {
        // Given: the first candidate draws a number of times, the second one records its first draw
        fun secondDraw(firstDraws: Int): Long {
            var calls = 0
            var second = 0L
            GardenComposer.choose(GardenMood.CALM, seed = 7, compose = { _, random ->
                when (calls++) {
                    0 -> repeat(firstDraws) { random.nextLong() }
                    1 -> second = random.nextLong()
                }
                gardenWithAnO(0)
            })
            return second
        }

        // Then
        assertThat(secondDraw(firstDraws = 50)).isEqualTo(secondDraw(firstDraws = 0))
    }

    @Test
    fun `when the 20 candidates of a seed are given up the composer fails`() {
        // Given
        var calls = 0
        val compose = { _: GardenMood, _: StableRandom -> null.also { calls++ } }

        // Then
        assertThatIllegalStateException().isThrownBy {
            GardenComposer.choose(GardenMood.CALM, seed = 7, compose = compose)
        }
        assertThat(calls).isEqualTo(20)
    }

    @Test
    fun `a candidate that does not place all its pieces is given up`() {
        // Given: no draw is allowed
        val attempts = 0

        // When
        val candidate = GardenComposer.candidate(GardenMood.CALM, StableRandom(1), attempts)

        // Then
        assertThat(candidate).isNull()
    }

    @Test
    fun `a candidate that places some pieces then misses one is given up`() {
        // Given: one draw per piece; with seed 1, the first piece is placed at its first draw
        val attempts = 1
        assertThat(GardenComposer.candidate(GardenMood.CALM, StableRandom(1), attempts, pieces = 1)).isNotNull

        // When
        val candidate = GardenComposer.candidate(GardenMood.CALM, StableRandom(1), attempts, pieces = 6)

        // Then
        assertThat(candidate).isNull()
    }

    @Test
    fun `every seed composes a garden of 6 pieces`() {
        // When
        val gardens = GardenMood.entries.flatMap { mood ->
            (1..100L).map { seed -> GardenComposer.compose(mood, seed) }
        }

        // Then: the garden accepts only pieces inside the grid, on free cells
        assertThat(gardens).allSatisfy { garden -> assertThat(garden.pieces).hasSize(6) }
    }

    private companion object {
        /** A calm garden holding a single O, its left column on [column]. */
        fun gardenWithAnO(column: Int): Garden {
            val type = PieceType.O
            val piece = Piece(type, ClassicRotationSystem.spawnOrientation(type), Position(column + 1, 1))
            return (Garden.EMPTY.place(piece) as GardenPlacement.Placed).garden
        }
    }
}
