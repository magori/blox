package ch.magori.blox.feature.garden

import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.feature.garden.sound.GARDEN_WARM_UP
import ch.magori.blox.feature.garden.sound.keptCuesOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** The sound of a garden kept in « Mes jardins », by the sound effects, as the settings want them. */
internal class GardenKeptSoundTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val kept = FakeGardenRepository()
    private val library = FakeGardenLibrary()
    private val soundEffects = FakeSoundEffectPlayer()
    private val preferences = FakeUserPreferencesRepository()

    // Built in each test, once the main dispatcher is the test one: it reads the settings at once.
    private val viewModel by lazy {
        gardenViewModel(kept, soundEffects = soundEffects, preferences = preferences, library = library)
    }

    @ParameterizedTest
    @EnumSource(GardenMood::class)
    fun `a garden kept plays the chord of its tonic as an arpeggio, when the settings want the sound effects`(
        mood: GardenMood,
    ) {
        // Given the window of the name open on a garden of that mood
        kept.state.value = A_GARDEN.withMood(mood)
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // When
        viewModel.confirmKeeping(messageMillis = 4_000)
        main.scheduler.advanceUntilIdle()

        // Then 3 notes, those of the arpeggio of its tonic, all of them warmed up as the garden opened
        assertThat(soundEffects.played).hasSize(3)
        assertThat(soundEffects.played).containsExactlyElementsOf(keptCuesOf(mood).map { it.effect })
        assertThat(GARDEN_WARM_UP).containsAll(soundEffects.played)
    }

    @Test
    fun `with the sound effects off, a garden kept plays nothing`() {
        // Given
        preferences.state.value = UserPreferences.DEFAULT.copy(soundEffects = false)
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // When
        viewModel.confirmKeeping(messageMillis = 4_000)
        main.scheduler.advanceUntilIdle()

        // Then
        assertThat(soundEffects.played).isEmpty()
    }

    private companion object {
        /** An I on the top row, heard in the joyful mood. */
        val A_GARDEN: Garden = Garden.parse(
            """
            IIII............
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            """,
        ).withMood(GardenMood.JOYFUL)
    }
}
