package ch.magori.blox.music

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.music.Scores
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SavedVolumesTest {
    private val music = FakeMusicPlayer()
    private val effects = FakeSoundEffectPlayer()

    @Test
    fun `a volume changed in the settings applies to the music playing`() = runTest {
        // Given a score playing at the volume saved
        val preferences = FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(musicVolume = Volume(70)))
        backgroundScope.applySavedVolumes(preferences, music, effects)
        music.play(Scores.korobeiniki)
        runCurrent()
        assertThat(music.volume).isEqualTo(Volume(70))

        // When the player turns it down in the settings
        preferences.setMusicVolume(Volume(20))
        runCurrent()

        // Then the score goes on, at the new volume
        assertThat(music.volume).isEqualTo(Volume(20))
        assertThat(music.playing).isEqualTo(Scores.korobeiniki.id)
    }

    @Test
    fun `the volume of the effects saved in the settings applies to the sound effects, apart from the music`() =
        runTest {
            // Given the volume of the effects saved, the music at its own
            val preferences = FakeUserPreferencesRepository(
                UserPreferences.DEFAULT.copy(musicVolume = Volume(70), effectsVolume = Volume(40)),
            )
            backgroundScope.applySavedVolumes(preferences, music, effects)
            runCurrent()
            assertThat(effects.volume).isEqualTo(Volume(40))

            // When the player changes it in the settings
            preferences.setEffectsVolume(Volume(90))
            runCurrent()

            // Then the effects play at it, the music at its own
            assertThat(effects.volume).isEqualTo(Volume(90))
            assertThat(music.volume).isEqualTo(Volume(70))
        }

    @Test
    fun `another setting changed applies no volume again`() = runTest {
        // Given the volumes saved, applied once each
        val preferences = FakeUserPreferencesRepository()
        backgroundScope.applySavedVolumes(preferences, music, effects)
        runCurrent()

        // When the player changes the theme
        preferences.setTheme(ThemeChoice.COLORFUL)
        runCurrent()

        // Then neither the music nor the effects are told their volume again
        assertThat(music.volumesSet).containsExactly(Volume.FULL)
        assertThat(effects.volumesSet).containsExactly(Volume.FULL)
    }
}
