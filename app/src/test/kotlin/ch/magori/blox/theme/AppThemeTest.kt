package ch.magori.blox.theme

import app.cash.turbine.test
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
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
        val preferences = FakeUserPreferencesRepository(initial = null)
        val appTheme = AppTheme(preferences, backgroundScope)

        appTheme.kind.test {
            // Then no theme, not even the default one, before the settings are read
            assertThat(awaitItem()).isNull()

            // When they are read, then the player chooses another theme
            preferences.state.value = UserPreferences.DEFAULT
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
        val preferences = FakeUserPreferencesRepository(initial = null)
        val appTheme = AppTheme(preferences, backgroundScope)
        preferences.state.value = UserPreferences.DEFAULT.copy(theme = ThemeChoice.COLORFUL)
        runCurrent()

        // When a new screen, such as a recreated activity, reads the theme
        val known = appTheme.kind.value

        // Then it has it at once, with no default theme first
        assertThat(known).isEqualTo(BloxThemeKind.COLORFUL)
    }

    @Test
    fun `changing the music or the start level does not emit the theme again`() = runTest {
        // Given the settings read
        val preferences = FakeUserPreferencesRepository(initial = null)
        preferences.state.value = UserPreferences.DEFAULT
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
}
