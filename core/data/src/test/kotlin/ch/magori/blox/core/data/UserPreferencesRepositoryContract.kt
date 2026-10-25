package ch.magori.blox.core.data

import app.cash.turbine.test
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * The contract of [UserPreferencesRepository], what every implementation does, the real one and the fake
 * shared by the tests of the features alike. A subclass gives an empty repository; what is proper to an
 * implementation (a file, its errors) is tested in the subclass.
 */
abstract class UserPreferencesRepositoryContract {
    /** A repository without any saved settings, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyRepository(scope: CoroutineScope): UserPreferencesRepository

    @Test
    fun `without saved preferences the defaults are the Original theme, Korobeiniki, level 0 and vibrations off`() =
        runTest {
            // Given
            val repository = emptyRepository(backgroundScope)

            // When
            val preferences = repository.preferences.first()

            // Then
            assertThat(preferences).isEqualTo(
                UserPreferences(
                    theme = ThemeChoice.ORIGINAL,
                    music = MusicChoice.KOROBEINIKI,
                    startLevel = 0,
                    vibrations = false,
                    soundEffects = true,
                    reducedEffects = false,
                    leftHanded = false,
                    keyVibration = false,
                    musicVolume = Volume.FULL,
                    effectsVolume = Volume.FULL,
                ),
            )
        }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ThemeChoice::class)
    fun `a saved theme is read back`(theme: ThemeChoice) = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.setTheme(theme)

        // Then
        assertThat(repository.preferences.first().theme).isEqualTo(theme)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(MusicChoice::class)
    fun `a saved music is read back`(music: MusicChoice) = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.setMusic(music)

        // Then
        assertThat(repository.preferences.first().music).isEqualTo(music)
    }

    @ParameterizedTest(name = "level {0}")
    @ValueSource(ints = [0, 5, 9])
    fun `a saved start level is read back`(startLevel: Int) = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.setStartLevel(startLevel)

        // Then
        assertThat(repository.preferences.first().startLevel).isEqualTo(startLevel)
    }

    @ParameterizedTest(name = "vibrations {0}")
    @ValueSource(booleans = [false, true])
    fun `a saved vibrations choice is read back`(vibrations: Boolean) = runTest {
        // Given the vibrations saved the other way first, so that the choice read back is not there by chance
        val repository = emptyRepository(backgroundScope)
        repository.setVibrations(!vibrations)

        // When
        repository.setVibrations(vibrations)

        // Then
        assertThat(repository.preferences.first().vibrations).isEqualTo(vibrations)
    }

    @Test
    fun `without saved preferences the sound effects are on`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences.soundEffects).isTrue()
    }

    @Test
    fun `without saved preferences effects are not reduced, right handed, keys do not vibrate, volumes at 100`() =
        runTest {
            // Given
            val repository = emptyRepository(backgroundScope)

            // When
            val preferences = repository.preferences.first()

            // Then
            assertThat(preferences.reducedEffects).isFalse()
            assertThat(preferences.leftHanded).isFalse()
            assertThat(preferences.keyVibration).isFalse()
            assertThat(preferences.musicVolume).isEqualTo(Volume.FULL)
            assertThat(preferences.effectsVolume).isEqualTo(Volume.FULL)
        }

    @Test
    fun `each new setting saved is read back`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When each new setting is saved away from its default, the two volumes apart
        repository.setReducedEffects(true)
        repository.setLeftHanded(true)
        repository.setKeyVibration(true)
        repository.setMusicVolume(Volume(30))
        repository.setEffectsVolume(Volume(70))

        // Then
        assertThat(repository.preferences.first()).isEqualTo(
            UserPreferences.DEFAULT.copy(
                reducedEffects = true,
                leftHanded = true,
                keyVibration = true,
                musicVolume = Volume(30),
                effectsVolume = Volume(70),
            ),
        )
    }

    @ParameterizedTest(name = "sound effects {0}")
    @ValueSource(booleans = [false, true])
    fun `a saved sound effects choice is read back`(soundEffects: Boolean) = runTest {
        // Given the sound effects saved the other way first, so that the choice read back is not there by chance
        val repository = emptyRepository(backgroundScope)
        repository.setSoundEffects(!soundEffects)

        // When
        repository.setSoundEffects(soundEffects)

        // Then
        assertThat(repository.preferences.first().soundEffects).isEqualTo(soundEffects)
    }

    @Test
    fun `changing one preference leaves the others`() = runTest {
        // Given all the preferences saved, none at its default
        val repository = emptyRepository(backgroundScope)
        repository.setTheme(ThemeChoice.COLORFUL)
        repository.setMusic(MusicChoice.MINUET)
        repository.setStartLevel(7)
        repository.setVibrations(true)
        repository.setSoundEffects(false)
        repository.setReducedEffects(true)
        repository.setLeftHanded(true)
        repository.setKeyVibration(true)
        repository.setMusicVolume(Volume(40))
        repository.setEffectsVolume(Volume.SILENT)

        // When the theme changes
        repository.setTheme(ThemeChoice.ORIGINAL)

        // Then all the others are kept
        assertThat(repository.preferences.first()).isEqualTo(
            UserPreferences(
                theme = ThemeChoice.ORIGINAL,
                music = MusicChoice.MINUET,
                startLevel = 7,
                vibrations = true,
                soundEffects = false,
                reducedEffects = true,
                leftHanded = true,
                keyVibration = true,
                musicVolume = Volume(40),
                effectsVolume = Volume.SILENT,
            ),
        )
    }

    @Test
    fun `a start level of 15 is read back as is`() = runTest {
        // Given: the range of the start levels belongs to the game mode, not to the settings
        val repository = emptyRepository(backgroundScope)

        // When
        repository.setStartLevel(15)

        // Then
        assertThat(repository.preferences.first().startLevel).isEqualTo(15)
    }

    @Test
    fun `a negative start level is refused and nothing is saved`() = runTest {
        // Given a start level of 5 saved
        val repository = emptyRepository(backgroundScope)
        repository.setStartLevel(5)

        // When
        val failure = runCatching { repository.setStartLevel(-1) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repository.preferences.first().startLevel).isEqualTo(5)
    }

    @Test
    fun `the preferences flow emits each change once, and nothing when a value is saved again`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.preferences.test {
            assertThat(awaitItem()).isEqualTo(UserPreferences.DEFAULT)

            // When the theme changes, then is saved again unchanged, then the music changes
            repository.setTheme(ThemeChoice.COLORFUL)
            val afterTheme = awaitItem()
            repository.setTheme(ThemeChoice.COLORFUL)
            repository.setMusic(MusicChoice.MINUET)

            // Then
            assertThat(afterTheme.theme).isEqualTo(ThemeChoice.COLORFUL)
            assertThat(
                awaitItem(),
            ).isEqualTo(UserPreferences.DEFAULT.copy(theme = ThemeChoice.COLORFUL, music = MusicChoice.MINUET))
            expectNoEvents()
        }
    }
}
