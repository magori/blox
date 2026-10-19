package ch.magori.blox.core.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
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
class DataStoreUserPreferencesRepositoryTest : UserPreferencesRepositoryContract() {
    @TempDir
    lateinit var directory: File

    @Test
    fun `an unknown value in the file gives the default of that preference`() = runTest {
        // Given a file written by another version: a theme and a music it no longer knows, a negative level
        writeSettingsFile {
            it[stringPreferencesKey("theme")] = "RAINBOW"
            it[stringPreferencesKey("music")] = "JAZZ"
            it[intPreferencesKey("start_level")] = -3
        }
        val repository = repositoryIn(backgroundScope)

        // When
        val preferences = repository.preferences.first()

        // Then
        assertThat(preferences).isEqualTo(
            UserPreferences(theme = ThemeChoice.ORIGINAL, music = MusicChoice.KOROBEINIKI, startLevel = 0),
        )
    }

    @Test
    fun `a corrupted settings file gives the defaults, and a new choice is saved`() = runTest {
        // Given a settings file that is not a preferences file
        settingsFile.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.preferences.first()).isEqualTo(UserPreferences.DEFAULT)
        repository.setMusic(MusicChoice.NONE)
        assertThat(repository.preferences.first().music).isEqualTo(MusicChoice.NONE)
    }

    @Test
    fun `a settings file that cannot be read gives the defaults`() = runTest {
        // Given a directory where the settings file should be: reading it fails with an IOException
        settingsFile.mkdirs()
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.preferences.first()).isEqualTo(UserPreferences.DEFAULT)
    }

    @Test
    fun `a failure other than an IOException while the settings are read is not absorbed`() = runTest {
        // Given a DataStore whose reading fails with a bug
        val repository = DataStoreUserPreferencesRepository(FailingDataStore(IllegalStateException("a bug")))

        // When
        val failure = runCatching { repository.preferences.first() }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug")
    }

    @Test
    fun `a change of a key the settings do not use emits nothing`() = runTest {
        // Given a repository on a DataStore the test can also write to
        val dataStore = preferencesDataStore(backgroundScope) { settingsFile.absolutePath.toPath() }
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
            }
        }

        // When
        val reopened = repositoryIn(backgroundScope)

        // Then
        assertThat(reopened.preferences.first()).isEqualTo(
            UserPreferences(theme = ThemeChoice.COLORFUL, music = MusicChoice.MY_FILE, startLevel = 9),
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
        preferencesDataStore(scope) { settingsFile.absolutePath.toPath() }.edit(write)
    }

    /** Runs [block] in a scope of its own, then closes it: the DataStores living in it release the file. */
    private suspend fun TestScope.inScopeClosedAfter(block: suspend (CoroutineScope) -> Unit) {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        block(scope)
        scope.coroutineContext.job.cancelAndJoin()
    }

    /** The settings file of the test, in [directory]. */
    private val settingsFile: File get() = File(directory, SETTINGS_FILE)

    /** A repository on the settings file of [directory], its DataStore living in [scope], as in production. */
    private fun repositoryIn(scope: CoroutineScope): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(preferencesDataStore(scope) { settingsFile.absolutePath.toPath() })

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
            argumentSet("NONE", "NONE", MusicChoice.NONE),
        )
    }
}
