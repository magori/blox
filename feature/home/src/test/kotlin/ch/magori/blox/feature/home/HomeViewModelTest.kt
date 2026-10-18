package ch.magori.blox.feature.home

import app.cash.turbine.test
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.rules.ClassicLevelProgression
import ch.magori.blox.core.game.rules.LevelProgression
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HomeViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun `the home screen is loading until the saved settings are read, then shows them and the record`() = runTest {
        // Given settings not read yet
        val preferences = FakeUserPreferencesRepository(initial = null)
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(initial = 12_345), ClassicLevelProgression)

        viewModel.uiState.test {
            // Then nothing is shown, and nothing can be played, before they are read
            assertThat(awaitItem()).isNull()

            // When they are read
            preferences.store(UserPreferences(ThemeChoice.COLORFUL, MusicChoice.MINUET, 5))

            // Then
            assertThat(awaitItem()).isEqualTo(
                HomeUiState(
                    theme = ThemeChoice.COLORFUL,
                    music = MusicChoice.MINUET,
                    startLevel = 5,
                    startLevels = (0..9).toList(),
                    record = 12_345,
                ),
            )
        }
    }

    @Test
    fun `choosing a theme, a music or a start level saves it`() = runTest {
        // Given
        val preferences = FakeUserPreferencesRepository()
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), ClassicLevelProgression)

        viewModel.uiState.test {
            // When
            viewModel.onThemeChosen(ThemeChoice.COLORFUL)
            viewModel.onMusicChosen(MusicChoice.NONE)
            viewModel.onStartLevelChosen(7)

            // Then
            assertThat(preferences.saved).isEqualTo(UserPreferences(ThemeChoice.COLORFUL, MusicChoice.NONE, 7))
            val shown = expectMostRecentItem()
            assertThat(shown?.theme).isEqualTo(ThemeChoice.COLORFUL)
            assertThat(shown?.music).isEqualTo(MusicChoice.NONE)
            assertThat(shown?.startLevel).isEqualTo(7)
        }
    }

    @Test
    fun `a choice that cannot be saved leaves the saved settings shown, without crashing`() = runTest {
        // Given a disk that refuses every write
        val preferences = FakeUserPreferencesRepository(failWrites = true)
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), ClassicLevelProgression)

        viewModel.uiState.test {
            // When
            viewModel.onThemeChosen(ThemeChoice.COLORFUL)
            viewModel.onMusicChosen(MusicChoice.NONE)
            viewModel.onStartLevelChosen(7)

            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.theme).isEqualTo(ThemeChoice.ORIGINAL)
            assertThat(shown?.music).isEqualTo(MusicChoice.KOROBEINIKI)
            assertThat(shown?.startLevel).isEqualTo(0)
        }
    }

    @Test
    fun `a start level out of the levels of the mode is not saved`() = runTest {
        // Given
        val preferences = FakeUserPreferencesRepository()
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), ClassicLevelProgression)

        // When
        viewModel.onStartLevelChosen(12)

        // Then
        assertThat(preferences.saved?.startLevel).isEqualTo(0)
    }

    @ParameterizedTest(name = "saved level {0} -> shown level {1}")
    @CsvSource("4, 4", "9, 5", "0, 3")
    fun `the levels offered are those of the mode, and a saved level out of them is shown brought into them`(
        savedLevel: Int,
        shownLevel: Int,
    ) = runTest {
        // Given a mode that starts at levels 3 to 5
        val preferences = FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(startLevel = savedLevel))
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), startingAt(3, 4, 5))

        // When / Then
        viewModel.uiState.test {
            val shown = expectMostRecentItem()
            assertThat(shown?.startLevels).containsExactly(3, 4, 5)
            assertThat(shown?.startLevel).isEqualTo(shownLevel)
        }
    }

    @ParameterizedTest(name = "saved level {0} -> shown level {1}")
    @CsvSource("3, 5", "2, 0", "8, 10", "12, 10")
    fun `a saved level between the levels of the mode is shown as the closest of them`(
        savedLevel: Int,
        shownLevel: Int,
    ) = runTest {
        // Given a mode that starts at levels 0, 5 and 10
        val preferences = FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(startLevel = savedLevel))
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), startingAt(0, 5, 10))

        // When / Then
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.startLevel).isEqualTo(shownLevel)
        }
    }

    @Test
    fun `a mode without start levels is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { HomeViewModel(FakeUserPreferencesRepository(), FakeHighScoreRepository(), startingAt()) }
            .withMessageContaining("start level")
    }

    /** A mode whose games may start at [levels] only. */
    private fun startingAt(vararg levels: Int): LevelProgression = object : LevelProgression {
        override val startLevels: List<Level> = levels.map(::Level)

        override fun levelFor(
            start: Level,
            lines: Lines,
        ): Level = start
    }
}
