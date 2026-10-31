package ch.magori.blox.music

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.music.SoundStyle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SavedSoundStyleTest {
    private val music = FakeMusicPlayer()

    @Test
    fun `the saved style is applied to the music as soon as it is read, then at each change`() = runTest {
        // Given the Velvet style saved
        val preferences = FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(soundStyle = SoundStyle.VELVET))

        // When it is applied, as the application starts
        backgroundScope.applySavedSoundStyle(preferences, music)
        runCurrent()

        // Then the music takes it, then the one the player chooses in the settings
        assertThat(music.soundStyle).isEqualTo(SoundStyle.VELVET)
        preferences.setSoundStyle(SoundStyle.NEON)
        runCurrent()
        assertThat(music.soundStyle).isEqualTo(SoundStyle.NEON)
    }

    @Test
    fun `a change of another setting does not set the style again`() = runTest {
        // Given the style saved, applied once
        val preferences = FakeUserPreferencesRepository()
        backgroundScope.applySavedSoundStyle(preferences, music)
        runCurrent()

        // When the player changes the theme
        preferences.setTheme(ThemeChoice.COLORFUL)
        runCurrent()

        // Then the music is not told its style again
        assertThat(music.soundStylesSet).containsExactly(SoundStyle.ARCADE)
    }
}
