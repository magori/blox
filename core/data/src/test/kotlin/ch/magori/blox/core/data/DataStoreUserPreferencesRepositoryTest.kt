package ch.magori.blox.core.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

/** The contract of the settings, on a DataStore file, and what is proper to a file: it is kept, its errors, its keys. */
class DataStoreUserPreferencesRepositoryTest :
    UserPreferencesRepositoryContract(),
    DataStoreFileContract<UserPreferencesRepository> {
    @TempDir
    lateinit var directory: File

    @Test
    fun `an unknown value in the file gives the default of that preference`() = runTest {
        // Given a file written by another version: a theme and a music it no longer knows, a negative level,
        // volumes that are not one of the steps of 10 from 0 to 100
        writeSettingsFile {
            it[stringPreferencesKey("theme")] = "RAINBOW"
            it[stringPreferencesKey("music")] = "JAZZ"
            it[intPreferencesKey("start_level")] = -3
            it[intPreferencesKey("music_volume")] = 55
            it[intPreferencesKey("effects_volume")] = 150
        }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences).isEqualTo(UserPreferences.DEFAULT)
    }

    @Test
    fun `settings saved before the vibrations existed keep their choices, with vibrations off`() = runTest {
        // Given a file written by a version without the vibrations setting
        writeSettingsFile {
            it[stringPreferencesKey("theme")] = "COLORFUL"
            it[stringPreferencesKey("music")] = "MINUET"
            it[intPreferencesKey("start_level")] = 7
        }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences).isEqualTo(
            UserPreferences.DEFAULT.copy(
                theme = ThemeChoice.COLORFUL,
                music = MusicChoice.MINUET,
                startLevel = 7,
                vibrations = false,
            ),
        )
    }

    @Test
    fun `the vibrations are stored as a boolean under a key that must not change`() = runTest {
        // Given a file whose vibrations were turned on by an earlier version
        writeSettingsFile { it[booleanPreferencesKey("vibrations")] = true }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences.vibrations).isTrue()
    }

    @Test
    fun `settings saved before the sound effects existed keep their choices, with sound effects on`() = runTest {
        // Given a file written by a version without the sound effects setting
        writeSettingsFile {
            it[stringPreferencesKey("theme")] = "COLORFUL"
            it[booleanPreferencesKey("vibrations")] = true
        }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences).isEqualTo(
            UserPreferences.DEFAULT.copy(theme = ThemeChoice.COLORFUL, vibrations = true, soundEffects = true),
        )
    }

    @Test
    fun `the sound effects are stored as a boolean under a key that must not change`() = runTest {
        // Given a file whose sound effects were turned off by an earlier version: off is not the default, on is
        writeSettingsFile { it[booleanPreferencesKey("sound_effects")] = false }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences.soundEffects).isFalse()
    }

    @Test
    fun `settings saved before Couleurs v2 keep their choices`() = runTest {
        // Given a file written before the settings of Couleurs v2, its sound effects turned off by the player
        writeSettingsFile {
            it[stringPreferencesKey("theme")] = "COLORFUL"
            it[stringPreferencesKey("music")] = "NONE"
            it[intPreferencesKey("start_level")] = 4
            it[booleanPreferencesKey("vibrations")] = true
            it[booleanPreferencesKey("sound_effects")] = false
        }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then the sound effects stay off, although they are now on by default; the new settings are at theirs
        assertThat(preferences).isEqualTo(
            UserPreferences.DEFAULT.copy(
                theme = ThemeChoice.COLORFUL,
                music = MusicChoice.NONE,
                startLevel = 4,
                vibrations = true,
                soundEffects = false,
            ),
        )
    }

    @Test
    fun `each new setting is stored under a key that must not change`() = runTest {
        // Given a file holding each new setting away from its default, under its name
        writeSettingsFile {
            it[booleanPreferencesKey("reduced_effects")] = true
            it[booleanPreferencesKey("left_handed")] = true
            it[booleanPreferencesKey("key_vibration")] = true
            it[intPreferencesKey("music_volume")] = 20
            it[intPreferencesKey("effects_volume")] = 60
        }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences).isEqualTo(
            UserPreferences.DEFAULT.copy(
                reducedEffects = true,
                leftHanded = true,
                keyVibration = true,
                musicVolume = Volume(20),
                effectsVolume = Volume(60),
            ),
        )
    }

    @Test
    fun `a change of a key the settings do not use emits nothing`() = runTest {
        // Given a repository on a DataStore the test can also write to
        val dataStore = preferencesDataStore(backgroundScope) { file.absolutePath.toPath() }
        val repository = DataStoreUserPreferencesRepository(dataStore)

        repository.preferences.test {
            assertThat(awaitItem()).isEqualTo(UserPreferences.DEFAULT)

            // When another key of the file changes
            dataStore.edit { it[stringPreferencesKey("other")] = "anything" }
            testScheduler.advanceUntilIdle()

            // Then the settings did not change: nothing is emitted
            expectNoEvents()
        }
    }

    @Test
    fun `the preferences survive a new repository on the same file`() = runTest {
        // Given preferences saved by a repository whose DataStore is then closed
        inScopeClosedAfter { scope ->
            repositoryIn(scope).apply {
                setTheme(ThemeChoice.COLORFUL)
                setMusic(MusicChoice.MY_FILE)
                setStartLevel(9)
                setVibrations(true)
                setSoundEffects(false)
                setReducedEffects(true)
                setLeftHanded(true)
                setKeyVibration(true)
                setMusicVolume(Volume(80))
                setEffectsVolume(Volume(10))
            }
        }

        // When
        val reopened = repositoryIn(backgroundScope)

        // Then
        assertThat(reopened.preferences.first()).isEqualTo(
            UserPreferences(
                theme = ThemeChoice.COLORFUL,
                music = MusicChoice.MY_FILE,
                startLevel = 9,
                vibrations = true,
                soundEffects = false,
                reducedEffects = true,
                leftHanded = true,
                keyVibration = true,
                musicVolume = Volume(80),
                effectsVolume = Volume(10),
            ),
        )
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("storedThemes")
    fun `the theme is stored by its name, which must not change`(
        stored: String,
        theme: ThemeChoice,
    ) = runTest {
        // Given
        writeSettingsFile { it[stringPreferencesKey("theme")] = stored }

        // When / Then
        assertThat(repositoryIn(backgroundScope).preferences.first().theme).isEqualTo(theme)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("storedMusics")
    fun `the music is stored by its name, which must not change`(
        stored: String,
        music: MusicChoice,
    ) = runTest {
        // Given
        writeSettingsFile { it[stringPreferencesKey("music")] = stored }

        // When / Then
        assertThat(repositoryIn(backgroundScope).preferences.first().music).isEqualTo(music)
    }

    override fun emptyRepository(scope: CoroutineScope): UserPreferencesRepository = repositoryIn(scope)

    /** Writes the settings file with a DataStore of its own, closed before the test goes on. */
    private suspend fun TestScope.writeSettingsFile(write: (MutablePreferences) -> Unit) = inScopeClosedAfter { scope ->
        preferencesDataStore(scope) { file.absolutePath.toPath() }.edit(write)
    }

    /** The settings file of the test, in [directory]. */
    override val file: File get() = File(directory, SETTINGS_FILE)

    override fun repositoryIn(scope: CoroutineScope): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(preferencesDataStore(scope) { file.absolutePath.toPath() })

    override fun repositoryFailingWith(failure: Exception): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(FailingDataStore(failure))

    override fun readsOf(repository: UserPreferencesRepository): List<Flow<Any?>> = listOf(repository.preferences)

    /** The defaults. */
    override val emptyReads: List<Any?> = listOf(UserPreferences.DEFAULT)

    /** A choice of no music, which is saved. */
    override suspend fun write(repository: UserPreferencesRepository) = repository.setMusic(MusicChoice.NONE)

    override val readsAfterWrite: List<Any?> = listOf(UserPreferences.DEFAULT.copy(music = MusicChoice.NONE))

    private companion object {
        /** The names saved files hold: a renamed constant would lose the choice of every player. */
        @JvmStatic
        fun storedThemes(): List<Arguments> = listOf(
            argumentSet("ORIGINAL", "ORIGINAL", ThemeChoice.ORIGINAL),
            argumentSet("COLORFUL", "COLORFUL", ThemeChoice.COLORFUL),
        )

        @JvmStatic
        fun storedMusics(): List<Arguments> = listOf(
            argumentSet("KOROBEINIKI", "KOROBEINIKI", MusicChoice.KOROBEINIKI),
            argumentSet("MINUET", "MINUET", MusicChoice.MINUET),
            argumentSet("MY_FILE", "MY_FILE", MusicChoice.MY_FILE),
            argumentSet("LIVE", "LIVE", MusicChoice.LIVE),
            argumentSet("NONE", "NONE", MusicChoice.NONE),
        )
    }
}
