package ch.magori.blox.feature.garden

import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.GardenSounds
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.feature.garden.sound.GARDEN_WARM_UP
import ch.magori.blox.feature.garden.sound.previewCuesOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** The sound of the preview, by the sound effects, as the settings want them (docs/jardin-musical.md, Son). */
class GardenPreviewSoundTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val soundEffects = FakeSoundEffectPlayer()
    private val preferences = FakeUserPreferencesRepository()

    // Built in each test, once the main dispatcher is the test one: it reads the settings at once.
    private val viewModel by lazy { gardenViewModel(soundEffects = soundEffects, preferences = preferences) }

    @Test
    fun `the notes of the garden are warmed up as it opens, when the settings want the sound effects`() {
        // When
        viewModel

        // Then
        assertThat(soundEffects.warmedUp).containsExactlyElementsOf(GARDEN_WARM_UP)
    }

    @Test
    fun `presenting the preview plays its notes once, as a rising arpeggio`() {
        // Given: the T chosen, pointing down, in the calm mood: G3 for its bar on row 10, E3 for its foot on row 11
        viewModel

        // When
        viewModel.tap(Position(5, 10))
        val atOnce = soundEffects.played.toList()
        main.scheduler.advanceTimeBy(GardenSounds.NOTE_STEP_MILLIS.toLong())
        main.scheduler.runCurrent()

        // Then
        assertThat(atOnce).containsExactly(GardenSounds.note(Pitch.parse("E3")))
        assertThat(soundEffects.played).containsExactly(
            GardenSounds.note(Pitch.parse("E3")),
            GardenSounds.note(Pitch.parse("G3")),
        )
    }

    @Test
    fun `moving, turning or choosing another piece plays the notes of the new preview`() {
        // Given a preview shown
        viewModel.tap(Position(5, 10))
        main.scheduler.advanceUntilIdle()

        // When it is moved, turned, then another piece is chosen
        val expected = mutableListOf<SoundEffect>()
        fun heard() = viewModel.uiState.value!!.preview!!.let { previewCuesOf(it, GardenMood.CALM) }.map { it.effect }
        soundEffects.played.clear()
        viewModel.tap(Position(8, 4))
        expected += heard()
        main.scheduler.advanceUntilIdle()
        viewModel.turn(Rotation.CLOCKWISE)
        expected += heard()
        main.scheduler.advanceUntilIdle()
        viewModel.choose(PieceType.I)
        expected += heard()
        main.scheduler.advanceUntilIdle()

        // Then
        assertThat(soundEffects.played).containsExactlyElementsOf(expected)
    }

    @Test
    fun `a touch that leaves the preview where it is plays nothing more`() {
        // Given a preview refused off the grid, its pivot on column 0, heard
        viewModel.tap(Position(0, 10))
        main.scheduler.advanceUntilIdle()
        soundEffects.played.clear()

        // When its pivot is touched again: a refused preview moves to the cell touched, here where it is
        viewModel.tap(Position(0, 10))
        main.scheduler.advanceUntilIdle()

        // Then
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `a new preview cuts the notes of the one before not yet heard`() {
        // Given a preview whose arpeggio has begun
        viewModel.tap(Position(5, 10))

        // When it is moved before its second note
        viewModel.tap(Position(5, 2))
        main.scheduler.advanceUntilIdle()

        // Then the first note of the first, then the whole second: C5 and D5 in the calm mood
        assertThat(soundEffects.played).containsExactly(
            GardenSounds.note(Pitch.parse("E3")),
            GardenSounds.note(Pitch.parse("C5")),
            GardenSounds.note(Pitch.parse("D5")),
        )
    }

    @Test
    fun `placing the preview, Retirer or a change of the garden play no preview`() {
        // Given a preview shown, heard
        viewModel.tap(Position(5, 10))
        main.scheduler.advanceUntilIdle()
        soundEffects.played.clear()

        // When
        viewModel.place()
        viewModel.tap(Position(5, 2))
        main.scheduler.advanceUntilIdle()
        soundEffects.played.clear()
        viewModel.remove()
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.clearAll()
        main.scheduler.advanceUntilIdle()

        // Then
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `with the sound effects off, nothing is warmed up nor played`() {
        // Given
        preferences.state.value = UserPreferences.DEFAULT.copy(soundEffects = false)

        // When
        viewModel.tap(Position(5, 10))
        main.scheduler.advanceUntilIdle()

        // Then
        assertThat(soundEffects.warmedUp).isEmpty()
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `taking a piece back plays the notes of the preview it becomes`() {
        // Given a T placed
        viewModel.tap(Position(5, 10))
        viewModel.place()
        main.scheduler.advanceUntilIdle()
        soundEffects.played.clear()

        // When it is taken back
        viewModel.tap(Position(5, 10))
        main.scheduler.advanceUntilIdle()

        // Then
        assertThat(soundEffects.played).containsExactly(
            GardenSounds.note(Pitch.parse("E3")),
            GardenSounds.note(Pitch.parse("G3")),
        )
        assertThat(viewModel.uiState.value?.garden).isEqualTo(Garden.EMPTY)
    }
}
