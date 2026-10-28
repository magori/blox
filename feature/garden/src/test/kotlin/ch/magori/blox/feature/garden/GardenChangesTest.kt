package ch.magori.blox.feature.garden

import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenComposer
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.GardenPlacement
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.random.Random

/** The changes of the whole garden: the mood, Tout effacer, Au hasard, and Annuler for 4 s (docs, Écran). */
class GardenChangesTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val kept = FakeGardenRepository().apply { state.value = A_GARDEN }

    // Built in each test, once the main dispatcher is the test one: it reads the garden kept at once.
    private val viewModel by lazy { gardenViewModel(kept) }

    @Test
    fun `choosing a mood keeps the pieces where they are, heard in that mood, and is kept`() {
        // When
        viewModel.chooseMood(GardenMood.EPIC)

        // Then
        val expected = A_GARDEN.withMood(GardenMood.EPIC)
        assertThat(viewModel.uiState.value?.garden).isEqualTo(expected)
        assertThat(kept.state.value).isEqualTo(expected)
    }

    @Test
    fun `Tout effacer empties the garden in its mood, kept, and offers Annuler`() {
        // When
        viewModel.clearAll()

        // Then
        val state = viewModel.uiState.value
        assertThat(state?.garden).isEqualTo(A_GARDEN.clear())
        assertThat(kept.state.value).isEqualTo(A_GARDEN.clear())
        assertThat(state?.undo).isEqualTo(A_GARDEN)
    }

    @Test
    fun `Annuler gives the garden from before back, kept, and is no longer offered`() {
        // Given
        viewModel.clearAll()

        // When
        viewModel.undo()

        // Then
        val state = viewModel.uiState.value
        assertThat(state?.garden).isEqualTo(A_GARDEN)
        assertThat(kept.state.value).isEqualTo(A_GARDEN)
        assertThat(state?.undo).isNull()
    }

    @Test
    fun `Annuler is offered for 4 seconds`() {
        // Given
        viewModel.clearAll()

        // When almost 4 s go by, then the rest
        main.scheduler.advanceTimeBy(3_999)
        main.scheduler.runCurrent()
        val before = viewModel.uiState.value?.undo
        main.scheduler.advanceTimeBy(1)
        main.scheduler.runCurrent()

        // Then
        assertThat(before).isEqualTo(A_GARDEN)
        assertThat(viewModel.uiState.value?.undo).isNull()
        assertThat(viewModel.uiState.value?.garden).isEqualTo(A_GARDEN.clear())
    }

    @Test
    fun `Au hasard composes a garden in the mood from a seed drawn for this roll, kept, and offers Annuler`() {
        // Given seeds drawn from a known generator
        val viewModel = gardenViewModel(kept, random = Random(SEEDS))

        // When
        viewModel.roll()

        // Then
        val composed = GardenComposer.compose(GardenMood.JOYFUL, Random(SEEDS).nextLong())
        val state = viewModel.uiState.value
        assertThat(state?.garden).isEqualTo(composed)
        assertThat(kept.state.value).isEqualTo(composed)
        assertThat(state?.undo).isEqualTo(A_GARDEN)
    }

    @Test
    fun `another change of the garden ends Annuler, so that it never undoes more than the garden cleared`() {
        // Given
        viewModel.clearAll()

        // When a piece is placed
        viewModel.tap(Position(1, 1))
        viewModel.place()

        // Then
        assertThat(viewModel.uiState.value?.undo).isNull()
    }

    @Test
    fun `choosing a mood or taking a piece back ends Annuler, as any change of the garden`() {
        // Given Annuler offered after Au hasard
        viewModel.roll()

        // When a piece of the composed garden is taken back, then after Tout effacer the mood is changed
        val piece = viewModel.uiState.value!!.garden.pieces.first()
        viewModel.tap(piece.pivot)
        val afterTakingBack = viewModel.uiState.value?.undo
        viewModel.clearAll()
        viewModel.chooseMood(GardenMood.CALM)

        // Then
        assertThat(afterTakingBack).isNull()
        assertThat(viewModel.uiState.value?.undo).isNull()
    }

    @Test
    fun `choosing a piece, turning it or Retirer keep Annuler offered, the garden unchanged`() {
        // Given
        viewModel.clearAll()
        viewModel.tap(Position(1, 1))

        // When
        viewModel.choose(PieceType.L)
        viewModel.turn(Rotation.CLOCKWISE)
        viewModel.remove()

        // Then
        assertThat(viewModel.uiState.value?.undo).isEqualTo(A_GARDEN)
    }

    @Test
    fun `Au hasard on an empty garden does not offer Annuler, nothing was cleared`() {
        // Given an empty garden
        kept.state.value = Garden.EMPTY

        // When
        viewModel.roll()

        // Then a garden is composed, without Annuler
        assertThat(viewModel.uiState.value?.garden?.pieces).isNotEmpty()
        assertThat(viewModel.uiState.value?.undo).isNull()
    }

    @Test
    fun `nothing is changed nor kept while the garden kept is being read`() {
        // Given a garden kept that is never read
        val written = mutableListOf<Garden>()
        val unread = object : GardenRepository {
            override val garden: Flow<Garden> = flow { awaitCancellation() }

            override suspend fun keep(garden: Garden) {
                written += garden
            }
        }
        val viewModel = gardenViewModel(unread)

        // When the player acts meanwhile
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.tap(Position(1, 1))
        viewModel.place()
        viewModel.clearAll()
        viewModel.roll()
        viewModel.undo()

        // Then the screen still waits, and nothing is kept in place of the garden being read
        assertThat(viewModel.uiState.value).isNull()
        assertThat(written).isEmpty()
    }

    private companion object {
        /** The seed of the generator the rolls draw their seeds from. */
        const val SEEDS = 42

        /** A T on (5, 6), in the joyful mood. */
        val A_GARDEN: Garden = (
            Garden.EMPTY.place(
                Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 6)),
            ) as GardenPlacement.Placed
            ).garden.withMood(GardenMood.JOYFUL)
    }
}
