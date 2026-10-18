package ch.magori.blox.theme

import app.cash.turbine.test
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AppThemeTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource("ORIGINAL, ORIGINAL", "COLORFUL, COLORFUL")
    fun `each theme chosen on the home screen has its theme in the design system`(
        choice: ThemeChoice,
        expected: BloxThemeKind,
    ) {
        assertThat(themeKindOf(choice)).isEqualTo(expected)
    }

    @Test
    fun `the theme of the application follows the saved theme, and shows none before it is read`() = runTest {
        // Given settings not read yet
        val preferences = InMemoryPreferences()
        val appTheme = AppTheme(preferences, backgroundScope)

        appTheme.kind.test {
            // Then no theme, not even the default one, before the settings are read
            assertThat(awaitItem()).isNull()

            // When they are read, then the player chooses another theme
            preferences.stored.value = UserPreferences.DEFAULT
            val first = awaitItem()
            preferences.setTheme(ThemeChoice.COLORFUL)

            // Then
            assertThat(first).isEqualTo(BloxThemeKind.ORIGINAL)
            assertThat(awaitItem()).isEqualTo(BloxThemeKind.COLORFUL)
        }
    }

    @Test
    fun `once read, the theme is known at once to a new collector`() = runTest {
        // Given the settings read once, as when the application starts
        val preferences = InMemoryPreferences()
        val appTheme = AppTheme(preferences, backgroundScope)
        preferences.stored.value = UserPreferences.DEFAULT.copy(theme = ThemeChoice.COLORFUL)
        runCurrent()

        // When a new screen, such as a recreated activity, reads the theme
        val known = appTheme.kind.value

        // Then it has it at once, with no default theme first
        assertThat(known).isEqualTo(BloxThemeKind.COLORFUL)
    }

    @Test
    fun `changing the music or the start level does not emit the theme again`() = runTest {
        // Given the settings read
        val preferences = InMemoryPreferences()
        preferences.stored.value = UserPreferences.DEFAULT
        val appTheme = AppTheme(preferences, backgroundScope)
        runCurrent()

        appTheme.kind.test {
            assertThat(awaitItem()).isEqualTo(BloxThemeKind.ORIGINAL)

            // When
            preferences.setMusic(MusicChoice.NONE)
            preferences.setStartLevel(7)

            // Then
            expectNoEvents()
        }
    }

    /** Settings kept in memory, not read until [stored] holds a value. */
    private class InMemoryPreferences : UserPreferencesRepository {
        val stored = MutableStateFlow<UserPreferences?>(null)

        override val preferences: Flow<UserPreferences> = stored.filterNotNull()

        override suspend fun setTheme(theme: ThemeChoice) {
            stored.value = stored.value?.copy(theme = theme)
        }

        override suspend fun setMusic(music: MusicChoice) {
            stored.value = stored.value?.copy(music = music)
        }

        override suspend fun setStartLevel(startLevel: Int) {
            stored.value = stored.value?.copy(startLevel = startLevel)
        }
    }
}
