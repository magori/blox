package ch.magori.blox.feature.musics

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

internal class MyMusicsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val library = BreakableMusicLibrary()

    private val music = FakeMusicPlayer()

    @Test
    fun `the list shows the saved musics in their order, by their name, category and tempo`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir", Category.CALM, bpm = 80)
        val handMade = aSavedMusic("Ma marche", category = null, bpm = 132)
        val viewModel = myMusics()

        // When / Then
        viewModel.uiState.test {
            assertThat(expectMostRecentItem()?.musics).containsExactly(
                MusicCard(evening.id, "Thème du soir", Category.CALM, bpm = 80, inGame = false),
                MusicCard(handMade.id, "Ma marche", category = null, bpm = 132, inGame = false),
            )
        }
    }

    @Test
    fun `the music played during the games is in game, and named at the top, the others are not`() = runMyMusicsTest {
        // Given
        aSavedMusic("Thème du soir")
        val march = aSavedMusic("Ma marche")
        library.setGameMusic(march.id)
        val viewModel = myMusics()

        // When / Then
        viewModel.uiState.test {
            val shown = expectMostRecentItem()
            assertThat(shown?.musics?.map { it.name to it.inGame }).containsExactly(
                "Thème du soir" to false,
                "Ma marche" to true,
            )
            assertThat(shown?.gameMusic).isEqualTo("Ma marche")
        }
    }

    @Test
    fun `without a music for the games, none is in game and the top names none`() = runMyMusicsTest {
        // Given
        aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        // When / Then
        viewModel.uiState.test {
            val shown = expectMostRecentItem()
            assertThat(shown?.musics?.single()?.inGame).isFalse()
            assertThat(shown?.gameMusic).isNull()
        }
    }

    @Test
    fun `Utiliser plays the music during the games, in place of the one played so far`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val march = aSavedMusic("Ma marche")
        library.setGameMusic(evening.id)
        val viewModel = myMusics()

        // When
        viewModel.onUse(march.id)

        // Then
        assertThat(library.gameMusic.first()?.id).isEqualTo(march.id)
    }

    @Test
    fun `En jeu touched plays the music chosen on the home screen during the games again`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        library.setGameMusic(evening.id)
        val viewModel = myMusics()

        // When
        viewModel.onUse(evening.id)

        // Then
        assertThat(library.gameMusic.first()).isNull()
    }

    @Test
    fun `a music listened to plays its score, and its card shows the first bar of eight`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            // When
            viewModel.onListen(evening.id)

            // Then
            assertThat(music.playing).isEqualTo(evening.id)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening).isEqualTo(Listening(bar = 1, bars = 8))
        }
    }

    @Test
    fun `the bar shown follows the tempo of the music, and comes back to the first after the last`() = runMyMusicsTest {
        // Given a music of four beats a bar at 120 beats a minute: two seconds a bar
        val evening = aSavedMusic("Thème du soir", bpm = 120)
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onListen(evening.id)

            // When / Then
            advance(1_999)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening?.bar).isEqualTo(1)
            advance(1)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening?.bar).isEqualTo(2)
            advance(12_000)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening?.bar).isEqualTo(8)
            advance(2_000)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening?.bar).isEqualTo(1)
        }
    }

    @Test
    fun `a tempo whose bars last no whole number of milliseconds does not drift`() = runMyMusicsTest {
        // Given a music at 140 beats a minute: a bar of 1 714,29 ms, eight bars in 13 714,29 ms
        val evening = aSavedMusic("Thème du soir", bpm = 140)
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onListen(evening.id)

            // When / Then: 40 loops later, the first bar comes back at 548 571 ms, not one millisecond early or late
            advance(548_570)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening?.bar).isEqualTo(8)
            advance(1)
            assertThat(expectMostRecentItem()?.musics?.single()?.listening?.bar).isEqualTo(1)
        }
    }

    @Test
    fun `a music listened to again stops, and another one listened to takes its place`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val march = aSavedMusic("Ma marche")
        val viewModel = myMusics()

        viewModel.uiState.test {
            // When
            viewModel.onListen(evening.id)
            viewModel.onListen(march.id)

            // Then
            assertThat(music.playing).isEqualTo(march.id)
            assertThat(expectMostRecentItem()?.musics?.map { it.listening != null }).containsExactly(false, true)

            // When
            viewModel.onListen(march.id)

            // Then
            assertThat(music.playing).isNull()
            assertThat(expectMostRecentItem()?.musics?.map { it.listening }).containsOnlyNulls()
        }
    }

    @Test
    fun `a music that stops by itself offers to listen to it again`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onListen(evening.id)

            // When the audio output fails, or the audio focus is lost for good
            music.fail()

            // Then
            assertThat(expectMostRecentItem()?.musics?.single()?.listening).isNull()
        }
    }

    @Test
    fun `leaving the list stops the music listened to`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        myMusics().onListen(evening.id)

        // When the entry leaves the stack
        stores.single().clear()

        // Then
        assertThat(music.calls).containsExactly("play ${evening.id}", "stop")
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Hiding::class)
    fun `the music listened to stops once the list is no longer seen`(hiding: Hiding) = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onListen(evening.id)

            // When
            hiding.hide(viewModel)

            // Then
            assertThat(music.playing).isNull()
            assertThat(expectMostRecentItem()?.musics?.single()?.listening).isNull()
        }
    }

    @Test
    fun `a pause of the list alone, behind a dialog or in a split screen, lets the music listened to play on`() =
        runMyMusicsTest {
            // Given
            val evening = aSavedMusic("Thème du soir")
            val viewModel = myMusics()
            viewModel.onListen(evening.id)

            // When
            viewModel.onLifecycleEvent(Lifecycle.Event.ON_PAUSE)

            // Then
            assertThat(music.playing).isEqualTo(evening.id)
        }

    /** The ways the list is no longer seen, the entry still on the stack. */
    enum class Hiding(val hide: (MyMusicsViewModel) -> Unit) {
        /** The application in the background, or the screen off. */
        STOPPED({ it.onLifecycleEvent(Lifecycle.Event.ON_STOP) }),

        /** *Nouvelle* or *Au hasard*: the editor opens over the list, and plays its own music. */
        EDITOR_OPENED({ it.onEditorOpened() }),
    }

    @Test
    fun `Renommer opens the dialog on the name of the music`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            // When
            viewModel.onRename(evening.id)

            // Then
            assertThat(expectMostRecentItem()?.dialog).isEqualTo(MyMusicsDialog.Rename(evening.id, "Thème du soir"))
        }
    }

    @ParameterizedTest(name = "« {0} »: {1}")
    @CsvSource(
        "Thème du matin, ",
        "' Thème du matin ', ",
        "THÈME DU SOIR, ",
        "ma marche, TAKEN",
        "' Ma marche  ', TAKEN",
        "'', BLANK",
        "'   ', BLANK",
        "Une musique dont le nom a plus de 40 lettres, TOO_LONG",
    )
    fun `a name is refused when blank, taken by another music, the case and the spaces ignored, or too long`(
        name: String,
        problem: NameProblem?,
    ) = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        aSavedMusic("Ma marche")
        val viewModel = myMusics()

        // When
        viewModel.onRename(evening.id)

        // Then
        assertThat(viewModel.renameProblem(name)).isEqualTo(problem)
    }

    @Test
    fun `a free name renames the music, without the spaces around it, and closes the dialog`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onRename(evening.id)

            // When
            viewModel.onRenamed("  Thème du matin ")

            // Then
            assertThat(library.musics.first().single().name).isEqualTo("Thème du matin")
            assertThat(expectMostRecentItem()?.dialog).isNull()
        }
    }

    @Test
    fun `a name refused renames nothing, and leaves the dialog open`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        aSavedMusic("Ma marche")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onRename(evening.id)

            // When
            viewModel.onRenamed("MA MARCHE")

            // Then
            assertThat(library.musics.first().map { it.name }).containsExactly("Thème du soir", "Ma marche")
            assertThat(expectMostRecentItem()?.dialog).isEqualTo(MyMusicsDialog.Rename(evening.id, "Thème du soir"))
        }
    }

    @Test
    fun `the dialog closed renames nothing`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onRename(evening.id)

            // When
            viewModel.onDialogDismissed()

            // Then
            assertThat(expectMostRecentItem()?.dialog).isNull()
            assertThat(library.musics.first().single().name).isEqualTo("Thème du soir")
        }
    }

    @ParameterizedTest(name = "in game: {0}")
    @ValueSource(booleans = [false, true])
    fun `Supprimer asks to confirm, telling whether the music is played during the games`(inGame: Boolean) =
        runMyMusicsTest {
            // Given
            val evening = aSavedMusic("Thème du soir")
            if (inGame) library.setGameMusic(evening.id)
            val viewModel = myMusics()

            viewModel.uiState.test {
                // When
                viewModel.onDelete(evening.id)

                // Then
                assertThat(expectMostRecentItem()?.dialog)
                    .isEqualTo(MyMusicsDialog.ConfirmDelete(evening.id, "Thème du soir", inGame))
                assertThat(library.musics.first()).hasSize(1)
            }
        }

    @Test
    fun `Garder la musique deletes nothing`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onDelete(evening.id)

            // When
            viewModel.onDialogDismissed()

            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.dialog).isNull()
            assertThat(shown?.deleted).isNull()
            assertThat(library.musics.first()).hasSize(1)
        }
    }

    @Test
    fun `the deletion confirmed deletes the music, and offers to undo it`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        aSavedMusic("Ma marche")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onDelete(evening.id)

            // When
            viewModel.onDeleteConfirmed()

            // Then
            val shown = expectMostRecentItem()
            assertThat(shown?.musics?.map { it.name }).containsExactly("Ma marche")
            assertThat(shown?.dialog).isNull()
            assertThat(shown?.deleted).isEqualTo("Thème du soir")
        }
    }

    @Test
    fun `deleting the music listened to stops it, deleting another one lets it play`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val march = aSavedMusic("Ma marche")
        val viewModel = myMusics()
        viewModel.onListen(evening.id)

        // When
        viewModel.onDelete(march.id)
        viewModel.onDeleteConfirmed()

        // Then
        assertThat(music.playing).isEqualTo(evening.id)

        // When
        viewModel.onDelete(evening.id)
        viewModel.onDeleteConfirmed()

        // Then
        assertThat(music.playing).isNull()
    }

    @Test
    fun `Annuler puts the music deleted back at its place, played during the games again if it was`() =
        runMyMusicsTest {
            // Given
            val evening = aSavedMusic("Thème du soir")
            aSavedMusic("Ma marche")
            library.setGameMusic(evening.id)
            val viewModel = myMusics()

            viewModel.uiState.test {
                viewModel.onDelete(evening.id)
                viewModel.onDeleteConfirmed()

                // When
                viewModel.onUndo()

                // Then
                val shown = expectMostRecentItem()
                assertThat(shown?.musics?.map { it.name to it.inGame }).containsExactly(
                    "Thème du soir" to true,
                    "Ma marche" to false,
                )
                assertThat(shown?.deleted).isNull()
            }
        }

    @ParameterizedTest(name = "{0}")
    @EnumSource(NextAction::class)
    fun `the next action, or the message closed, ends the chance to undo the deletion`(next: NextAction) =
        runMyMusicsTest {
            // Given
            val evening = aSavedMusic("Thème du soir")
            val march = aSavedMusic("Ma marche")
            val viewModel = myMusics()

            viewModel.uiState.test {
                viewModel.onDelete(evening.id)
                viewModel.onDeleteConfirmed()

                // When
                next.act(viewModel, march.id)
                viewModel.onUndo()

                // Then
                assertThat(expectMostRecentItem()?.deleted).isNull()
                assertThat(library.musics.first().map { it.name }).containsExactly("Ma marche")
            }
        }

    @Test
    fun `a music whose name another one took since its deletion comes back under a free name`() = runMyMusicsTest {
        // Given a music deleted, and another one saved under its name from another screen
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onDelete(evening.id)
            viewModel.onDeleteConfirmed()
            aSavedMusic("THÈME DU SOIR")

            // When
            viewModel.onUndo()

            // Then
            assertThat(expectMostRecentItem()?.deleted).isNull()
            assertThat(library.musics.first().map { it.name }).containsExactly("Thème du soir 2", "THÈME DU SOIR")
        }
    }

    @Test
    fun `Annuler touched twice puts the music back once`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onDelete(evening.id)
            viewModel.onDeleteConfirmed()

            // When
            viewModel.onUndo()
            viewModel.onUndo()

            // Then
            assertThat(expectMostRecentItem()?.deleted).isNull()
            assertThat(library.musics.first().map { it.name }).containsExactly("Thème du soir")
        }
    }

    @Test
    fun `a music that cannot be put back keeps its message, to try again`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()

        viewModel.uiState.test {
            viewModel.onDelete(evening.id)
            viewModel.onDeleteConfirmed()
            library.broken = setOf(BreakableMusicLibrary.Write.RESTORE)

            // When
            viewModel.onUndo()

            // Then
            assertThat(expectMostRecentItem()?.deleted).isEqualTo("Thème du soir")
            assertThat(library.musics.first()).isEmpty()

            // When the disk has room again
            library.broken = emptySet()
            viewModel.onUndo()

            // Then
            assertThat(expectMostRecentItem()?.deleted).isNull()
            assertThat(library.musics.first().map { it.name }).containsExactly("Thème du soir")
        }
    }

    @Test
    fun `a rename that cannot be written leaves the dialog open, telling it failed`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()
        library.broken = setOf(BreakableMusicLibrary.Write.RENAME)

        viewModel.uiState.test {
            viewModel.onRename(evening.id)

            // When
            viewModel.onRenamed("Thème du matin")

            // Then
            assertThat(expectMostRecentItem()?.dialog)
                .isEqualTo(MyMusicsDialog.Rename(evening.id, "Thème du soir", failed = true))
            assertThat(library.musics.first().single().name).isEqualTo("Thème du soir")
        }
    }

    @Test
    fun `a deletion that cannot be written leaves the confirmation open, telling it failed, and offers no undo`() =
        runMyMusicsTest {
            // Given
            val evening = aSavedMusic("Thème du soir")
            val viewModel = myMusics()
            library.broken = setOf(BreakableMusicLibrary.Write.DELETE)

            viewModel.uiState.test {
                viewModel.onListen(evening.id)
                viewModel.onDelete(evening.id)

                // When
                viewModel.onDeleteConfirmed()

                // Then
                val shown = expectMostRecentItem()
                assertThat(shown?.dialog)
                    .isEqualTo(MyMusicsDialog.ConfirmDelete(evening.id, "Thème du soir", inGame = false, failed = true))
                assertThat(shown?.deleted).isNull()
                assertThat(shown?.musics).hasSize(1)
                assertThat(music.playing).isEqualTo(evening.id)
            }
        }

    @Test
    fun `a choice for the games that cannot be written changes nothing`() = runMyMusicsTest {
        // Given
        val evening = aSavedMusic("Thème du soir")
        val viewModel = myMusics()
        library.broken = setOf(BreakableMusicLibrary.Write.SET_GAME_MUSIC)

        viewModel.uiState.test {
            // When
            viewModel.onUse(evening.id)

            // Then
            assertThat(expectMostRecentItem()?.musics?.single()?.inGame).isFalse()
        }
    }

    @Test
    fun `an action made while the deletion is being written ends the chance to undo it before it is offered`() =
        runMyMusicsTest {
            // Given a slow disk
            val evening = aSavedMusic("Thème du soir")
            val march = aSavedMusic("Ma marche")
            val viewModel = myMusics()
            val held = CompletableDeferred<Unit>()
            library.deletionHeld = held

            viewModel.uiState.test {
                viewModel.onDelete(evening.id)
                viewModel.onDeleteConfirmed()

                // When
                viewModel.onListen(march.id)
                held.complete(Unit)
                runCurrent()

                // Then
                val shown = expectMostRecentItem()
                assertThat(shown?.musics?.map { it.name }).containsExactly("Ma marche")
                assertThat(shown?.deleted).isNull()
            }
        }

    /** The actions that end the chance to undo a deletion: any action of the list, and the message closed. */
    enum class NextAction(val act: (MyMusicsViewModel, ScoreId.Personal) -> Unit) {
        LISTEN({ viewModel, other -> viewModel.onListen(other) }),
        USE({ viewModel, other -> viewModel.onUse(other) }),
        RENAME({ viewModel, other -> viewModel.onRename(other) }),
        DELETE({ viewModel, other -> viewModel.onDelete(other) }),
        EDITOR_OPENED({ viewModel, _ -> viewModel.onEditorOpened() }),
        MESSAGE_CLOSED({ viewModel, _ -> viewModel.onUndoDismissed() }),
    }

    /** The view model stores of the test, as the navigation entries hold them: cleared at its end. */
    private val stores = mutableListOf<ViewModelStore>()

    /**
     * Runs [body] as a test, then clears the view models it built ([myMusics]), as the entry leaving the stack does:
     * a music listened to stops, and its bars are no longer counted, which would never end on the virtual time.
     */
    private fun runMyMusicsTest(body: suspend TestScope.() -> Unit) = runTest {
        try {
            body()
        } finally {
            stores.forEach(ViewModelStore::clear)
        }
    }

    /** « Mes musiques » in a view model store like the navigation entry's, built by the factory as `app` builds it. */
    private fun myMusics(): MyMusicsViewModel {
        val store = ViewModelStore().also { stores += it }
        return ViewModelProvider.create(store, MyMusicsViewModel.Factory(library, music))[MyMusicsViewModel::class]
    }

    /** Lets [millis] milliseconds go by, running what is due by then, at that very time too. */
    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    /** Saves a music called [name], of [category] and of a score of eight bars at [bpm]. */
    private suspend fun aSavedMusic(
        name: String,
        category: Category? = Category.JOYFUL,
        bpm: Int = 120,
    ): SavedMusic {
        val id = library.newId()
        val saved = SavedMusic(id, name, category, Scale(Pitch.parse("C4"), ScaleType.MAJOR), eightBars(id, bpm))
        library.save(saved)
        return saved
    }

    /** A score of eight bars of four beats at [bpm], each bar one whole note of the melody over the bass. */
    private fun eightBars(
        id: ScoreId,
        bpm: Int,
    ): Score = Score(
        id = id,
        bpm = bpm,
        beatsPerBar = 4,
        voices = listOf(
            Voice(Waveform.SQUARE, 0.07, List(8) { Note(Pitch.parse("E4"), 4.0) }),
            Voice(Waveform.TRIANGLE, 0.16, List(8) { Note(Pitch.parse("C2"), 4.0) }),
        ),
    )
}
