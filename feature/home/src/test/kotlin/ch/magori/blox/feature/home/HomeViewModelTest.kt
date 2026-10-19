package ch.magori.blox.feature.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.rules.ClassicLevelProgression
import ch.magori.blox.core.game.rules.LevelProgression
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class HomeViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val music = FakeMusicPlayer()

    /** The scores of the music choices, as `app` gives them: Korobeiniki and the minuet, nothing else. */
    private val testScores: (MusicChoice) -> Score? =
        mapOf(MusicChoice.KOROBEINIKI to Scores.korobeiniki, MusicChoice.MINUET to Scores.minuet)::get

    @Test
    fun `the home screen is loading until the saved settings are read, then shows them and the record`() = runTest {
        // Given settings not read yet
        val preferences = FakeUserPreferencesRepository(initial = null)
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(initial = 12_345),
                ClassicLevelProgression,
                music,
                testScores,
            )

        viewModel.uiState.test {
            // Then nothing is shown, and nothing can be played, before they are read
            assertThat(awaitItem()).isNull()

            // When they are read
            preferences.state.value = UserPreferences(ThemeChoice.COLORFUL, MusicChoice.MINUET, 5)

            // Then
            assertThat(awaitItem()).isEqualTo(
                HomeUiState(
                    theme = ThemeChoice.COLORFUL,
                    music = MusicChoice.MINUET,
                    startLevel = 5,
                    startLevels = (0..9).toList(),
                    record = 12_345,
                    listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET),
                    previewing = null,
                ),
            )
        }
    }

    @Test
    fun `choosing a theme, a music or a start level saves it`() = runTest {
        // Given
        val preferences = FakeUserPreferencesRepository()
        val viewModel =
            HomeViewModel(preferences, FakeHighScoreRepository(), ClassicLevelProgression, music, testScores)

        viewModel.uiState.test {
            // When
            viewModel.onThemeChosen(ThemeChoice.COLORFUL)
            viewModel.onMusicChosen(MusicChoice.NONE)
            viewModel.onStartLevelChosen(7)

            // Then
            assertThat(preferences.state.value).isEqualTo(UserPreferences(ThemeChoice.COLORFUL, MusicChoice.NONE, 7))
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
        val viewModel =
            HomeViewModel(preferences, FakeHighScoreRepository(), ClassicLevelProgression, music, testScores)

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
    fun `unreadable records show 0 and the home screen goes on`() = runTest {
        // Given unreadable records: as the contract says, they give 0, once, and the flow then ends
        val preferences = FakeUserPreferencesRepository()
        val highScores = FakeHighScoreRepository(initial = 12_345).apply { highScoreFlow = flowOf(0) }
        val viewModel = HomeViewModel(preferences, highScores, ClassicLevelProgression, music, testScores)

        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.record).isEqualTo(0)

            // When the player goes on choosing
            viewModel.onThemeChosen(ThemeChoice.COLORFUL)

            // Then the home screen follows, with a record of 0
            val shown = awaitItem()
            assertThat(shown?.theme).isEqualTo(ThemeChoice.COLORFUL)
            assertThat(shown?.record).isEqualTo(0)
        }
    }

    @Test
    fun `a programming error while the records are read is not swallowed`() {
        // Given records whose reading fails with a bug, not with an unreadable file
        val bug = IllegalStateException("a bug in the records")
        val highScores = FakeHighScoreRepository().apply { highScoreFlow = flow { throw bug } }

        // When the home screen is shown; runTest reports the uncaught exceptions of viewModelScope
        val failure = runCatching {
            runTest {
                val viewModel = HomeViewModel(
                    FakeUserPreferencesRepository(),
                    highScores,
                    ClassicLevelProgression,
                    music,
                    testScores,
                )
                viewModel.uiState.test { cancelAndIgnoreRemainingEvents() }
            }
        }

        // Then the bug reaches the uncaught exceptions, as it would crash the application
        assertThat(failure.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java).hasMessage(bug.message)
    }

    @Test
    fun `a start level out of the levels of the mode is not saved`() = runTest {
        // Given
        val preferences = FakeUserPreferencesRepository()
        val viewModel =
            HomeViewModel(preferences, FakeHighScoreRepository(), ClassicLevelProgression, music, testScores)

        // When
        viewModel.onStartLevelChosen(12)

        // Then
        assertThat(preferences.state.value?.startLevel).isEqualTo(0)
    }

    @ParameterizedTest(name = "saved level {0} -> shown level {1}")
    @CsvSource("4, 4", "9, 5", "0, 3")
    fun `the levels offered are those of the mode, and a saved level out of them is shown brought into them`(
        savedLevel: Int,
        shownLevel: Int,
    ) = runTest {
        // Given a mode that starts at levels 3 to 5
        val preferences = FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(startLevel = savedLevel))
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), startingAt(3, 4, 5), music, testScores)

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
        val viewModel = HomeViewModel(preferences, FakeHighScoreRepository(), startingAt(0, 5, 10), music, testScores)

        // When / Then
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.startLevel).isEqualTo(shownLevel)
        }
    }

    @Test
    fun `a mode without start levels is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy {
                HomeViewModel(
                    FakeUserPreferencesRepository(),
                    FakeHighScoreRepository(),
                    startingAt(),
                    music,
                    testScores,
                )
            }
            .withMessageContaining("start level")
    }

    @Test
    fun `only the musics with a score can be listened to`() = runTest {
        // Given scores for the minuet and for no music, none for the others
        val viewModel = homeViewModel(
            scoreOf = mapOf(
                MusicChoice.MINUET to Scores.minuet,
                MusicChoice.NONE to Scores.korobeiniki,
            )::get,
        )

        // When / Then
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.listenable).containsExactly(MusicChoice.MINUET, MusicChoice.NONE)
        }
    }

    @Test
    fun `listening to a music plays its score and shows it as previewed`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When
            viewModel.onListen(MusicChoice.MINUET)

            // Then
            assertThat(music.calls).containsExactly("play MINUET")
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.MINUET)
        }
    }

    @Test
    fun `a preview plays at the normal tempo, though a game left in danger had sped the music up`() {
        // Given a game left while its stack was in danger: the music was asked to go a quarter faster
        music.setTempoFactor(1.25)
        val viewModel = homeViewModel()

        // When
        viewModel.onListen(MusicChoice.MINUET)

        // Then
        assertThat(music.tempo).isEqualTo(1.0)
    }

    @Test
    fun `a preview prepares no other tempo`() {
        // Given a game that prepared its danger tempo, then left
        music.prepareTempo(1.25)
        val viewModel = homeViewModel()

        // When
        viewModel.onListen(MusicChoice.MINUET)

        // Then the preview renders its music at its tempo only
        assertThat(music.prepared).isEmpty()
    }

    @Test
    fun `listening again to the music being previewed stops it`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onListen(MusicChoice.MINUET)

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop")
            assertThat(music.playing).isNull()
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `listening to another music stops the preview and plays the other`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onListen(MusicChoice.KOROBEINIKI)

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop", "play KOROBEINIKI")
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.KOROBEINIKI)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["NONE", "MY_FILE"])
    fun `a music without a BloX score cannot be listened to`(choice: MusicChoice) = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When
            viewModel.onListen(choice)

            // Then
            assertThat(music.calls).isEmpty()
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `choosing another music stops the preview`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onMusicChosen(MusicChoice.NONE)

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop")
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `choosing the music being previewed lets it play on`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onMusicChosen(MusicChoice.MINUET)

            // Then
            assertThat(music.calls).containsExactly("play MINUET")
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.MINUET)
        }
    }

    @Test
    fun `playing stops the preview`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onPlay()

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop")
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `the preview stops when the screen goes to the background, never the music of a game started since`() =
        runTest {
            // Given the minuet listened to, then a game that starts with Korobeiniki
            val viewModel = homeViewModel()
            viewModel.uiState.test {
                viewModel.onListen(MusicChoice.MINUET)
                music.play(Scores.korobeiniki)

                // When the home screen stops, behind the game screen
                viewModel.onLifecycleEvent(Lifecycle.Event.ON_STOP)

                // Then
                assertThat(music.calls).containsExactly("play MINUET", "play KOROBEINIKI", "stop")
                assertThat(music.playing).isEqualTo(ScoreId.KOROBEINIKI)
                assertThat(expectMostRecentItem()?.previewing).isNull()
            }
        }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["ON_STOP", "ON_DESTROY"], mode = EnumSource.Mode.EXCLUDE)
    fun `the preview plays on while the screen is still visible`(event: Lifecycle.Event) = runTest {
        // Given
        val viewModel = homeViewModel()
        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onLifecycleEvent(event)

            // Then
            assertThat(music.calls).containsExactly("play MINUET")
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.MINUET)
        }
    }

    @Test
    fun `leaving the home screen stops the preview`() {
        // Given the minuet listened to, in a view model store like the navigation entry's, built by the
        // factory with the music player of the application
        val store = ViewModelStore()
        homeViewModelIn(store).onListen(MusicChoice.MINUET)

        // When the entry leaves the stack
        store.clear()

        // Then
        assertThat(music.calls).containsExactly("play MINUET", "stop")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Leaving::class)
    fun `leaving the home screen with no music listened to lets the music of the game play, without any stop`(
        leaving: Leaving,
    ) {
        // Given a preview listened to then stopped, then a game that starts with Korobeiniki
        val store = ViewModelStore()
        val viewModel = homeViewModelIn(store)
        viewModel.onListen(MusicChoice.MINUET)
        viewModel.onListen(MusicChoice.MINUET)
        music.play(Scores.korobeiniki)

        // When
        leaving.leave(viewModel, store)

        // Then
        assertThat(music.calls).containsExactly("play MINUET", "stop", "play KOROBEINIKI")
        assertThat(music.playing).isEqualTo(ScoreId.KOROBEINIKI)
    }

    /** The ways the player leaves the home screen. */
    enum class Leaving(val leave: (HomeViewModel, ViewModelStore) -> Unit) {
        PLAY({ viewModel, _ -> viewModel.onPlay() }),
        ENTRY_CLEARED({ _, store -> store.clear() }),
    }

    /** A view model in [store], as in the navigation entry, built by the factory as `app` builds it. */
    private fun homeViewModelIn(store: ViewModelStore): HomeViewModel {
        val factory = HomeViewModel.Factory(
            FakeUserPreferencesRepository(),
            FakeHighScoreRepository(),
            ClassicLevelProgression,
            music,
            testScores,
        )
        return ViewModelProvider.create(store, factory)[HomeViewModel::class]
    }

    private fun homeViewModel(scoreOf: (MusicChoice) -> Score? = testScores): HomeViewModel = HomeViewModel(
        FakeUserPreferencesRepository(),
        FakeHighScoreRepository(),
        ClassicLevelProgression,
        music,
        scoreOf,
    )

    /** A mode whose games may start at [levels] only. */
    private fun startingAt(vararg levels: Int): LevelProgression = object : LevelProgression {
        override val startLevels: List<Level> = levels.map(::Level)

        override fun levelFor(
            start: Level,
            lines: Lines,
        ): Level = start
    }
}
