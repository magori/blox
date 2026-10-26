package ch.magori.blox.feature.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeMusicFileRepository
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.MusicFileRepository
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.rules.ClassicLevelProgression
import ch.magori.blox.core.game.rules.LevelProgression
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
import java.io.IOException

class HomeViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val musicFiles = FakeMusicFileRepository()

    private val library = FakeMusicLibrary()

    private val myFile = MusicFile("content://com.android.providers.media.documents/document/audio%3A42")

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
                musicFiles,
                library,
            )

        viewModel.uiState.test {
            // Then nothing is shown, and nothing can be played, before they are read
            assertThat(awaitItem()).isNull()

            // When they are read
            preferences.state.value =
                UserPreferences.DEFAULT.copy(
                    theme = ThemeChoice.COLORFUL,
                    music = MusicChoice.MINUET,
                    startLevel = 5,
                    vibrations = true,
                )

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
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                musicFiles,
                library,
            )

        viewModel.uiState.test {
            // When
            viewModel.onThemeChosen(ThemeChoice.COLORFUL)
            viewModel.onMusicChosen(MusicChoice.NONE)
            viewModel.onStartLevelChosen(7)

            // Then
            // The other settings, not changed, keep their defaults.
            val expected =
                UserPreferences.DEFAULT.copy(theme = ThemeChoice.COLORFUL, music = MusicChoice.NONE, startLevel = 7)
            assertThat(preferences.state.value).isEqualTo(expected)
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
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                musicFiles,
                library,
            )

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
        val viewModel =
            HomeViewModel(preferences, highScores, ClassicLevelProgression, music, testScores, musicFiles, library)

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
                    musicFiles,
                    library,
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
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                musicFiles,
                library,
            )

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
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                startingAt(3, 4, 5),
                music,
                testScores,
                musicFiles,
                library,
            )

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
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                startingAt(0, 5, 10),
                music,
                testScores,
                musicFiles,
                library,
            )

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
                    musicFiles,
                    library,
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
    fun `a preview whose music stops by itself, as the audio output fails, offers to listen again`() = runTest {
        // Given a music being listened to
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.MINUET)

            // When the music stops by itself
            music.fail()

            // Then the screen offers « Écouter » again, and listening plays the music again
            assertThat(viewModel.uiState.value?.previewing).isNull()
            // and the preview is stopped, which gives the audio focus back
            assertThat(music.calls).containsExactly("play MINUET", "failure", "stop")
            viewModel.onListen(MusicChoice.MINUET)
            assertThat(music.playing).isEqualTo(ScoreId.MINUET)
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.MINUET)
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
    fun `opening the music editor stops the preview`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onCompose()

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop")
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `opening my musics stops the preview`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onMyMusics()

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop")
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `changing the game stops the preview`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When the home leaves for the choice of the game: its view model is cleared, the preview would play on
            viewModel.onChangeGame()

            // Then
            assertThat(music.calls).containsExactly("play MINUET", "stop")
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `opening the settings stops the preview`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MINUET)

            // When
            viewModel.onSettings()

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

    @Test
    fun `the home screen is loading until the music file is read, so that its listen button never shows up late`() =
        runTest {
            // Given a music file not read yet
            val reading = MutableStateFlow<MusicFile?>(null)
            val unread = object : MusicFileRepository {
                override val musicFile: Flow<MusicFile?> = reading.drop(1)

                override suspend fun setMusicFile(file: MusicFile) = Unit
            }
            val viewModel = HomeViewModel(
                FakeUserPreferencesRepository(),
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                unread,
                library,
            )

            viewModel.uiState.test {
                // Then nothing is shown before it is read
                assertThat(awaitItem()).isNull()

                // When it is read
                reading.value = myFile

                // Then « Mon fichier » may be listened to from the first state shown
                assertThat(awaitItem()?.listenable).contains(MusicChoice.MY_FILE)
            }
        }

    @Test
    fun `my file can be listened to once a music file is chosen, in the order of the choices`() = runTest {
        // Given no music file yet
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.listenable).containsExactly(MusicChoice.KOROBEINIKI, MusicChoice.MINUET)

            // When a music file is chosen
            musicFiles.state.value = myFile

            // Then
            assertThat(awaitItem()?.listenable)
                .containsExactly(MusicChoice.KOROBEINIKI, MusicChoice.MINUET, MusicChoice.MY_FILE)
        }
    }

    @Test
    fun `listening to my file plays the music file chosen and shows it as previewed`() = runTest {
        // Given a music file chosen
        musicFiles.state.value = myFile
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When
            viewModel.onListen(MusicChoice.MY_FILE)

            // Then
            assertThat(music.calls).containsExactly("play file ${myFile.uri}")
            assertThat(expectMostRecentItem()?.previewing).isEqualTo(MusicChoice.MY_FILE)
        }
    }

    @Test
    fun `choosing my file with a music file kept chooses it without opening the picker`() = runTest {
        // Given a music file kept, and Korobeiniki chosen
        musicFiles.state.value = myFile
        val preferences = FakeUserPreferencesRepository()
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                musicFiles,
                library,
            )

        viewModel.uiState.test {
            // When
            viewModel.onMusicChosen(MusicChoice.MY_FILE)

            // Then
            assertThat(preferences.state.value?.music).isEqualTo(MusicChoice.MY_FILE)
            assertThat(viewModel.musicFilePickerRequested.value).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `choosing my file without a music file opens the picker, and chooses nothing yet`() = runTest {
        // Given no music file, and Korobeiniki chosen
        val preferences = FakeUserPreferencesRepository()
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                musicFiles,
                library,
            )

        viewModel.uiState.test {
            // When
            viewModel.onMusicChosen(MusicChoice.MY_FILE)

            // Then the choice waits for a file, which a picker cancelled never gives
            assertThat(viewModel.musicFilePickerRequested.value).isTrue()
            assertThat(preferences.state.value?.music).isEqualTo(MusicChoice.KOROBEINIKI)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing the music file opens the picker`() = runTest {
        // Given a music file kept
        musicFiles.state.value = myFile
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When
            viewModel.onChangeMusicFile()

            // Then
            assertThat(viewModel.musicFilePickerRequested.value).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the picker once opened is no longer asked for`() = runTest {
        // Given the picker asked for
        val viewModel = homeViewModel()
        viewModel.uiState.test {
            viewModel.onChangeMusicFile()

            // When the screen opens it
            viewModel.onMusicFilePickerOpened()

            // Then
            assertThat(viewModel.musicFilePickerRequested.value).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a music file kept is shown, so that it may be changed`() = runTest {
        // Given no music file
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.hasMusicFile).isFalse()

            // When one is kept
            musicFiles.state.value = myFile

            // Then
            assertThat(awaitItem()?.hasMusicFile).isTrue()
        }
    }

    @Test
    fun `choosing a music file keeps it and chooses my file`() = runTest {
        // Given Korobeiniki chosen
        val preferences = FakeUserPreferencesRepository()
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                musicFiles,
                library,
            )

        viewModel.uiState.test {
            // When the player picks a file in the picker of the system
            viewModel.onMusicFileChosen(myFile.uri)

            // Then
            assertThat(musicFiles.state.value).isEqualTo(myFile)
            assertThat(preferences.state.value?.music).isEqualTo(MusicChoice.MY_FILE)
            assertThat(expectMostRecentItem()?.music).isEqualTo(MusicChoice.MY_FILE)
        }
    }

    @Test
    fun `choosing a music file stops the preview, even that of the file before`() = runTest {
        // Given my file chosen and previewed
        musicFiles.state.value = myFile
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListen(MusicChoice.MY_FILE)

            // When another file is picked
            viewModel.onMusicFileChosen("content://music/other")

            // Then
            assertThat(music.calls).containsExactly("play file ${myFile.uri}", "stop")
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @Test
    fun `a music file that cannot be kept leaves the music chosen before`() = runTest {
        // Given Korobeiniki chosen, and a disk that refuses to keep the file
        val preferences = FakeUserPreferencesRepository()
        val files = FakeMusicFileRepository(failWrites = true)
        val viewModel =
            HomeViewModel(
                preferences,
                FakeHighScoreRepository(),
                ClassicLevelProgression,
                music,
                testScores,
                files,
                library,
            )

        viewModel.uiState.test {
            // When
            viewModel.onMusicFileChosen(myFile.uri)

            // Then
            assertThat(preferences.state.value?.music).isEqualTo(MusicChoice.KOROBEINIKI)
            assertThat(expectMostRecentItem()?.music).isEqualTo(MusicChoice.KOROBEINIKI)
        }
    }

    @Test
    fun `a music of the player played during the games is shown by its name, as the only music selected`() = runTest {
        // Given Korobeiniki chosen on the home screen, and a music of the player chosen for the games
        val evening = aGameMusic()
        val viewModel = homeViewModel()

        // When / Then
        viewModel.uiState.test {
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic).isEqualTo(GameMusicUi(name = "Thème du soir", previewed = false))
            assertThat(shown?.selectedMusic).isNull()
        }
    }

    @Test
    fun `without a music of the player for the games, the music chosen on the home screen is selected`() = runTest {
        // Given a music of the player saved, but none chosen for the games
        aSavedMusic("Thème du soir")
        val viewModel = homeViewModel()

        // When / Then
        viewModel.uiState.test {
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic).isNull()
            assertThat(shown?.selectedMusic).isEqualTo(MusicChoice.KOROBEINIKI)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["KOROBEINIKI", "MINUET", "NONE"])
    fun `choosing another music on the home screen leaves the music of the player and chooses it`(
        choice: MusicChoice,
    ) = runTest {
        // Given the minuet chosen on the home screen, and a music of the player chosen for the games
        val preferences = FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(music = MusicChoice.MINUET))
        aGameMusic()
        val viewModel = homeViewModelWith(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onMusicChosen(choice)

            // Then the games play the music chosen, even the one chosen before, or none
            assertThat(library.gameMusic.first()).isNull()
            assertThat(preferences.state.value?.music).isEqualTo(choice)
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic).isNull()
            assertThat(shown?.selectedMusic).isEqualTo(choice)
        }
    }

    @Test
    fun `choosing my file with a music file kept leaves the music of the player`() = runTest {
        // Given a music file kept, and a music of the player chosen for the games
        musicFiles.state.value = myFile
        val preferences = FakeUserPreferencesRepository()
        aGameMusic()
        val viewModel = homeViewModelWith(preferences)

        viewModel.uiState.test {
            // When
            viewModel.onMusicChosen(MusicChoice.MY_FILE)

            // Then
            assertThat(library.gameMusic.first()).isNull()
            assertThat(expectMostRecentItem()?.selectedMusic).isEqualTo(MusicChoice.MY_FILE)
        }
    }

    @Test
    fun `choosing a music file leaves the music of the player`() = runTest {
        // Given a music of the player chosen for the games
        val preferences = FakeUserPreferencesRepository()
        aGameMusic()
        val viewModel = homeViewModelWith(preferences)

        viewModel.uiState.test {
            // When the player picks a file in the picker of the system
            viewModel.onMusicFileChosen(myFile.uri)

            // Then
            assertThat(library.gameMusic.first()).isNull()
            assertThat(preferences.state.value?.music).isEqualTo(MusicChoice.MY_FILE)
            assertThat(expectMostRecentItem()?.selectedMusic).isEqualTo(MusicChoice.MY_FILE)
        }
    }

    @Test
    fun `choosing my file without a music file leaves the music of the player only once a file is picked`() = runTest {
        // Given no music file, and a music of the player chosen for the games
        aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When the player chooses « Mon fichier », and the picker opens
            viewModel.onMusicChosen(MusicChoice.MY_FILE)

            // Then the music of the player stays, as a picker cancelled leaves everything as it was
            assertThat(library.gameMusic.first()).isNotNull()
            assertThat(expectMostRecentItem()?.gameMusic).isNotNull()
        }
    }

    @Test
    fun `a music of the player that cannot be left stays selected, without crashing`() = runTest {
        // Given a music of the player chosen for the games, in a library that can no longer be written
        aGameMusic()
        val unwritable = object : MusicLibrary by library {
            override suspend fun setGameMusic(id: ScoreId.Personal?) = throw IOException("The disk is full")
        }
        val preferences = FakeUserPreferencesRepository()
        val viewModel = homeViewModelWith(preferences, musics = unwritable)

        viewModel.uiState.test {
            // When
            viewModel.onMusicChosen(MusicChoice.NONE)

            // Then the music of the home screen is saved already, the first write; but the games still play the music of
            // the player, and the screen shows it as before
            assertThat(preferences.state.value?.music).isEqualTo(MusicChoice.NONE)
            assertThat(library.gameMusic.first()?.name).isEqualTo("Thème du soir")
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic?.name).isEqualTo("Thème du soir")
            assertThat(shown?.selectedMusic).isNull()
        }
    }

    @Test
    fun `a music that cannot be chosen leaves the music of the player for the games`() = runTest {
        // Given a music of the player chosen for the games, and settings that can no longer be written
        aGameMusic()
        val viewModel = homeViewModelWith(FakeUserPreferencesRepository(failWrites = true))

        viewModel.uiState.test {
            // When
            viewModel.onMusicChosen(MusicChoice.NONE)

            // Then nothing changes
            assertThat(library.gameMusic.first()?.name).isEqualTo("Thème du soir")
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic?.name).isEqualTo("Thème du soir")
            assertThat(shown?.music).isEqualTo(MusicChoice.KOROBEINIKI)
        }
    }

    @Test
    fun `listening to the music of the player plays its score and shows it as previewed`() = runTest {
        // Given a music of the player chosen for the games
        val evening = aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When
            viewModel.onListenGameMusic()

            // Then
            assertThat(music.playing).isEqualTo(evening.id)
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic?.previewed).isTrue()
            assertThat(shown?.previewing).isNull()
        }
    }

    @Test
    fun `listening again to the music of the player stops it`() = runTest {
        // Given the music of the player listened to
        aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListenGameMusic()

            // When
            viewModel.onListenGameMusic()

            // Then
            assertThat(music.playing).isNull()
            assertThat(music.calls.last()).isEqualTo("stop")
            assertThat(expectMostRecentItem()?.gameMusic?.previewed).isFalse()
        }
    }

    @Test
    fun `opening the settings stops the music of the player listened to`() = runTest {
        // Given the music of the player listened to
        aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListenGameMusic()

            // When
            viewModel.onSettings()

            // Then
            assertThat(music.playing).isNull()
            assertThat(music.calls.last()).isEqualTo("stop")
            assertThat(expectMostRecentItem()?.gameMusic?.previewed).isFalse()
        }
    }

    @Test
    fun `listening to a music of the home screen stops the music of the player listened to`() = runTest {
        // Given the music of the player listened to
        aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListenGameMusic()

            // When
            viewModel.onListen(MusicChoice.MINUET)

            // Then
            assertThat(music.playing).isEqualTo(ScoreId.MINUET)
            val shown = expectMostRecentItem()
            assertThat(shown?.gameMusic?.previewed).isFalse()
            assertThat(shown?.previewing).isEqualTo(MusicChoice.MINUET)
        }
    }

    @Test
    fun `choosing another music stops the music of the player listened to`() = runTest {
        // Given the music of the player listened to
        aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListenGameMusic()

            // When
            viewModel.onMusicChosen(MusicChoice.MINUET)

            // Then
            assertThat(music.playing).isNull()
            assertThat(expectMostRecentItem()?.previewing).isNull()
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LeavingTheGames::class)
    fun `a music of the player listened to stops once it is no longer played during the games`(
        leaving: LeavingTheGames,
    ) = runTest {
        // Given the music of the player listened to
        val evening = aGameMusic()
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            viewModel.onListenGameMusic()

            // When another screen deletes it, or chooses another music for the games
            leaving.leave(library, evening)

            // Then
            assertThat(music.playing).isNotEqualTo(evening.id)
            assertThat(music.calls.last()).isEqualTo("stop")
            assertThat(expectMostRecentItem()?.gameMusic?.previewed ?: false).isFalse()
        }
    }

    @Test
    fun `without a music of the player for the games, there is nothing of it to listen to`() = runTest {
        // Given
        val viewModel = homeViewModel()

        viewModel.uiState.test {
            // When
            viewModel.onListenGameMusic()

            // Then
            assertThat(music.calls).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A music of the player called [name], saved in the library: the minuet, under a new personal identity. */
    private suspend fun aSavedMusic(name: String): SavedMusic {
        val id = library.newId()
        val saved = SavedMusic(id, name, null, Scale(Pitch.parse("C4"), ScaleType.MAJOR), Scores.minuet.copy(id = id))
        library.save(saved)
        return saved
    }

    /** A music of the player called « Thème du soir », saved in the library and chosen for the games. */
    private suspend fun aGameMusic(): SavedMusic = aSavedMusic("Thème du soir").also { library.setGameMusic(it.id) }

    /** The ways a music of the player stops being played during the games, from another screen. */
    enum class LeavingTheGames(val leave: suspend (FakeMusicLibrary, SavedMusic) -> Unit) {
        DELETED({ library, music -> library.delete(music.id) }),
        NO_LONGER_CHOSEN({ library, _ -> library.setGameMusic(null) }),
        REPLACED({ library, _ ->
            val id = library.newId()
            library.save(
                SavedMusic(
                    id,
                    "Autre",
                    null,
                    Scale(Pitch.parse("C4"), ScaleType.MAJOR),
                    Scores.korobeiniki.copy(id = id),
                ),
            )
            library.setGameMusic(id)
        }),
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
            musicFiles,
            library,
        )
        return ViewModelProvider.create(store, factory)[HomeViewModel::class]
    }

    private fun homeViewModel(scoreOf: (MusicChoice) -> Score? = testScores): HomeViewModel =
        homeViewModelWith(scoreOf = scoreOf)

    private fun homeViewModelWith(
        preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
        musics: MusicLibrary = library,
        scoreOf: (MusicChoice) -> Score? = testScores,
    ): HomeViewModel = HomeViewModel(
        preferences,
        FakeHighScoreRepository(),
        ClassicLevelProgression,
        music,
        scoreOf,
        musicFiles,
        musics,
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
