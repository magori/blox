package ch.magori.blox.feature.composer

import androidx.lifecycle.SavedStateHandle
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.SaveProblem
import ch.magori.blox.feature.composer.model.SaveStatus
import ch.magori.blox.feature.composer.model.pitch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

/** Saving, leaving, renaming and deleting the music of the editor, in the musics of the player. */
class ComposerSavingTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val music = FakeMusicPlayer()
    private val library = FakeMusicLibrary()
    private val viewModel by lazy { editor() }

    @Test
    fun `the window of a saved music opens on its name, and on whether it is played during the games`() = runTest {
        // Given
        library.saveMusic("Joyeux 1", key = "joyeux", forGames = true)
        val opened = editor(key = "joyeux")

        // When
        opened.onSaveAsked()

        // Then
        assertThat(opened.shown.dialog)
            .isEqualTo(ComposerDialog.Save(name = "Joyeux 1", forGames = true, nameChosen = true))
    }

    @Test
    fun `the window saves the music under its name and its identity, and the changes count from there`() = runTest {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()

        // When
        viewModel.onNameChanged("  Thème du soir ")
        viewModel.onSaveConfirmed()

        // Then
        val draft = viewModel.shown.editor.draft
        assertThat(library.musics.first())
            .containsExactly(SavedMusic(NEW_ID, "Thème du soir", null, draft.scale, draft.toScore(NEW_ID)))
        assertThat(viewModel.shown.editor.status).isEqualTo(SaveStatus.SAVED)
        assertThat(viewModel.shown.name).isEqualTo("Thème du soir")
        assertThat(viewModel.shown.dialog).isNull()
    }

    @Test
    fun `a music saved again replaces its saved version, under the same identity and name`() = runTest {
        // Given a music saved once, then changed
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.save(name = "Thème du soir")
        viewModel.onCellTouched(pitch("G4"), eighth = 2)

        // When
        viewModel.onSaveAsked()
        val window = viewModel.shown.dialog
        viewModel.onSaveConfirmed()

        // Then
        assertThat(window).isEqualTo(ComposerDialog.Save(name = "Thème du soir", forGames = false, nameChosen = true))
        val draft = viewModel.shown.editor.draft
        assertThat(library.musics.first()).singleElement().isEqualTo(
            SavedMusic(NEW_ID, "Thème du soir", null, draft.scale, draft.toScore(NEW_ID)),
        )
    }

    @Test
    fun `the window plays the music during the games when its box is checked, and no longer once unchecked`() =
        runTest {
            // Given
            viewModel.onCellTouched(pitch("E4"), eighth = 0)

            // When
            viewModel.onSaveAsked()
            viewModel.onForGamesChanged(true)
            viewModel.onSaveConfirmed()
            val chosen = library.gameMusic.first()?.id
            val shownChosen = viewModel.shown.isGameMusic
            viewModel.onSaveAsked()
            viewModel.onForGamesChanged(false)
            viewModel.onSaveConfirmed()

            // Then
            assertThat(chosen).isEqualTo(NEW_ID)
            assertThat(shownChosen).isTrue()
            assertThat(library.gameMusic.first()).isNull()
            assertThat(viewModel.shown.isGameMusic).isFalse()
        }

    @Test
    fun `the window refuses a name another music has, and offers a free one, which taking fills in`() = runTest {
        // Given
        library.saveMusic("Thème", key = "theme")
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()

        // When
        viewModel.onNameChanged("thème")
        val refused = viewModel.shown.dialog
        viewModel.onSaveConfirmed()
        viewModel.onFreeNameTaken()

        // Then
        assertThat(refused).isEqualTo(
            ComposerDialog.Save(
                name = "thème",
                forGames = false,
                nameChosen = true,
                problem = SaveProblem.Name(NameProblem.TAKEN),
                freeName = "thème 2",
            ),
        )
        assertThat(library.musics.first()).hasSize(1)
        assertThat(viewModel.shown.dialog)
            .isEqualTo(ComposerDialog.Save(name = "thème 2", forGames = false, nameChosen = true))
    }

    @Test
    fun `the window refuses an empty grid, and a blank name`() = runTest {
        // When
        viewModel.onSaveAsked()
        val empty = viewModel.shown.dialog
        viewModel.onSaveConfirmed()
        viewModel.onDialogDismissed()
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()
        viewModel.onNameChanged("  ")
        val blank = viewModel.shown.dialog
        viewModel.onSaveConfirmed()

        // Then
        assertThat((empty as ComposerDialog.Save).problem).isEqualTo(SaveProblem.EmptyGrid)
        assertThat((blank as ComposerDialog.Save).problem).isEqualTo(SaveProblem.Name(NameProblem.BLANK))
        assertThat(library.musics.first()).isEmpty()
    }

    @Test
    fun `a name typed is cut at 40 characters, the longest a saved name may be`() {
        // Given
        viewModel.onSaveAsked()

        // When
        viewModel.onNameChanged("é".repeat(45))

        // Then
        assertThat((viewModel.shown.dialog as ComposerDialog.Save).name).isEqualTo("é".repeat(40))
    }

    @Test
    fun `closing the window saves nothing`() = runTest {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()

        // When
        viewModel.onDialogDismissed()

        // Then
        assertThat(viewModel.shown.dialog).isNull()
        assertThat(library.musics.first()).isEmpty()
        assertThat(viewModel.shown.editor.status).isEqualTo(SaveStatus.NOT_SAVED)
    }

    @Test
    fun `a save that fails keeps the window open on the name typed and the box, says it failed, and saves nothing`() {
        // Given
        val failing = editor(library = FailingLibrary())
        failing.onCellTouched(pitch("E4"), eighth = 0)
        failing.onSaveAsked()
        failing.onNameChanged("Fête")
        failing.onForGamesChanged(true)

        // When
        failing.onSaveConfirmed()

        // Then
        assertThat(failing.shown.dialog)
            .isEqualTo(ComposerDialog.Save(name = "Fête", forGames = true, nameChosen = true, failed = true))
        assertThat(failing.shown.editor.status).isEqualTo(SaveStatus.NOT_SAVED)
        assertThat(failing.shown.message).isNull()
    }

    @Test
    fun `a music saved whose choice for the games fails stays saved, can be deleted, and the window says it failed`() =
        runTest {
            // Given musics that save, but cannot choose the music for the games
            val musics = FakeMusicLibrary()
            val failing = editor(library = GameMusicFailingLibrary(musics))
            failing.onCellTouched(pitch("E4"), eighth = 0)
            failing.onSaveAsked()
            val proposed = (failing.shown.dialog as ComposerDialog.Save).name
            failing.onForGamesChanged(true)

            // When
            failing.onSaveConfirmed()

            // Then
            assertThat(musics.musics.first().map { it.id }).containsExactly(NEW_ID)
            assertThat(failing.shown.editor.status).isEqualTo(SaveStatus.SAVED)
            assertThat(failing.shown.name).isEqualTo(proposed)
            assertThat((failing.shown.dialog as ComposerDialog.Save).failed).isTrue()
            failing.onDialogDismissed()
            failing.onDeleteAsked()
            assertThat(failing.shown.dialog).isEqualTo(ComposerDialog.Delete(isGameMusic = false))
        }

    @Test
    fun `a second tap on Save while the music is being written does nothing`() = runTest {
        // Given musics that take their time to write
        val slow = SlowLibrary(library)
        val editor = editor(library = slow)
        editor.onCellTouched(pitch("E4"), eighth = 0)
        editor.onSaveAsked()

        // When
        editor.onSaveConfirmed()
        editor.onSaveConfirmed()
        slow.gate.complete(Unit)

        // Then
        assertThat(slow.saves).isEqualTo(1)
        assertThat(library.musics.first()).hasSize(1)
    }

    @Test
    fun `leaving a new music left empty, or a saved music unchanged, leaves at once without a question`() = runTest {
        // Given
        library.saveMusic("Joyeux 1", key = "joyeux")
        val saved = editor(key = "joyeux")

        // When
        viewModel.onBackAsked()
        saved.onBackAsked()

        // Then
        assertThat(viewModel.shown.dialog).isNull()
        assertThat(viewModel.shown.exit).isEqualTo(ComposerExit.Leave)
        assertThat(saved.shown.exit).isEqualTo(ComposerExit.Leave)
    }

    @Test
    fun `leaving a changed music asks first, and Continue stays in the editor`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)

        // When
        viewModel.onBackAsked()
        val asked = viewModel.shown.dialog
        viewModel.onDialogDismissed()

        // Then
        assertThat(asked).isEqualTo(ComposerDialog.Leave)
        assertThat(viewModel.shown.dialog).isNull()
        assertThat(viewModel.shown.exit).isNull()
    }

    @Test
    fun `Leave without saving leaves the editor, and saves nothing`() = runTest {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onBackAsked()

        // When
        viewModel.onLeaveWithoutSaving()

        // Then
        assertThat(viewModel.shown.exit).isEqualTo(ComposerExit.Leave)
        assertThat(library.musics.first()).isEmpty()
    }

    @Test
    fun `Save in the question opens the save window, which leaves the editor once the music is saved`() = runTest {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onBackAsked()

        // When
        viewModel.onSaveBeforeLeaving()
        val window = viewModel.shown.dialog
        viewModel.onSaveConfirmed()

        // Then
        assertThat((window as ComposerDialog.Save).thenLeave).isTrue()
        assertThat(library.musics.first()).hasSize(1)
        assertThat(viewModel.shown.exit).isEqualTo(ComposerExit.Leave)
    }

    @Test
    fun `the exit, once the screen has left, is forgotten, so that the screen leaves once`() {
        // Given
        viewModel.onBackAsked()

        // When
        viewModel.onExitDone()

        // Then
        assertThat(viewModel.shown.exit).isNull()
    }

    @Test
    fun `Rename opens the window on the name of the music`() = runTest {
        // Given
        library.saveMusic("Joyeux 1", key = "joyeux")
        val saved = editor(key = "joyeux")

        // When
        saved.onRenameAsked()

        // Then
        assertThat(saved.shown.dialog).isEqualTo(ComposerDialog.Rename(name = "Joyeux 1"))
    }

    @Test
    fun `renaming a saved music renames it in the library, and leaves its changes unsaved`() = runTest {
        // Given a saved music, changed
        val draft = library.saveMusic("Joyeux 1", key = "joyeux")
        val saved = editor(key = "joyeux")
        saved.onCellTouched(pitch("G4"), eighth = 2)

        // When
        saved.onRenameAsked()
        saved.onNameChanged("Fête")
        saved.onRenameConfirmed()

        // Then
        val id = ScoreId.Personal("joyeux")
        assertThat(library.musics.first()).containsExactly(
            SavedMusic(id, "Fête", null, draft.scale, draft.toScore(id)),
        )
        assertThat(saved.shown.name).isEqualTo("Fête")
        assertThat(saved.shown.editor.status).isEqualTo(SaveStatus.MODIFIED)
        assertThat(saved.shown.dialog).isNull()
    }

    @Test
    fun `renaming a new music names it without saving it, and the save window then proposes that name`() = runTest {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)

        // When
        viewModel.onRenameAsked()
        viewModel.onNameChanged(" Fête ")
        viewModel.onRenameConfirmed()
        viewModel.onSaveAsked()

        // Then
        assertThat(library.musics.first()).isEmpty()
        assertThat(viewModel.shown.name).isEqualTo("Fête")
        assertThat(viewModel.shown.dialog)
            .isEqualTo(ComposerDialog.Save(name = "Fête", forGames = false, nameChosen = true))
    }

    @Test
    fun `the rename window refuses a blank name, or one another music has, and offers a free one`() = runTest {
        // Given
        library.saveMusic("Fête", key = "party")
        viewModel.onRenameAsked()

        // When
        viewModel.onNameChanged(" ")
        val blank = viewModel.shown.dialog
        viewModel.onNameChanged("FÊTE")
        val taken = viewModel.shown.dialog
        viewModel.onRenameConfirmed()

        // Then
        assertThat(blank).isEqualTo(ComposerDialog.Rename(name = " ", problem = SaveProblem.Name(NameProblem.BLANK)))
        assertThat(taken).isEqualTo(
            ComposerDialog.Rename(name = "FÊTE", problem = SaveProblem.Name(NameProblem.TAKEN), freeName = "FÊTE 2"),
        )
        assertThat(viewModel.shown.name).isNull()
    }

    @Test
    fun `Delete asks first, for a saved music only, and warns when the music is played during the games`() = runTest {
        // Given
        library.saveMusic("Joyeux 1", key = "joyeux", forGames = true)
        val saved = editor(key = "joyeux")

        // When
        viewModel.onDeleteAsked()
        saved.onDeleteAsked()

        // Then
        assertThat(viewModel.shown.dialog).isNull()
        assertThat(saved.shown.dialog).isEqualTo(ComposerDialog.Delete(isGameMusic = true))
    }

    @Test
    fun `deleting the music deletes it, and the editor leaves, handing what was deleted to restore it`() = runTest {
        // Given
        library.saveMusic("Calme 1", key = "calm")
        library.saveMusic("Joyeux 1", key = "joyeux", forGames = true)
        val saved = editor(key = "joyeux")
        saved.onDeleteAsked()

        // When
        saved.onDeleteConfirmed()

        // Then
        assertThat(library.musics.first().map { it.name }).containsExactly("Calme 1")
        val exit = saved.shown.exit as ComposerExit.Deleted
        assertThat(exit.music.music.name).isEqualTo("Joyeux 1")
        assertThat(exit.music.place).isEqualTo(1)
        assertThat(exit.music.wasGameMusic).isTrue()
    }

    @Test
    fun `Keep the music closes the question, and deletes nothing`() = runTest {
        // Given
        library.saveMusic("Joyeux 1", key = "joyeux")
        val saved = editor(key = "joyeux")
        saved.onDeleteAsked()

        // When
        saved.onDialogDismissed()

        // Then
        assertThat(saved.shown.dialog).isNull()
        assertThat(library.musics.first()).hasSize(1)
    }

    @Test
    fun `a rename or a deletion that fails keeps its window open, says it failed, and changes nothing`() = runTest {
        // Given a saved music, in musics that can no longer be written
        val musics = FakeMusicLibrary()
        musics.saveMusic("Joyeux 1", key = "joyeux")
        val failing = editor(key = "joyeux", library = FailingLibrary(musics))

        // When
        failing.onRenameAsked()
        failing.onNameChanged("Fête")
        failing.onRenameConfirmed()
        val renamed = failing.shown
        failing.onDialogDismissed()
        failing.onDeleteAsked()
        failing.onDeleteConfirmed()

        // Then
        assertThat(renamed.name).isEqualTo("Joyeux 1")
        assertThat(renamed.dialog).isEqualTo(ComposerDialog.Rename(name = "Fête", failed = true))
        assertThat(failing.shown.dialog).isEqualTo(ComposerDialog.Delete(isGameMusic = false, failed = true))
        assertThat(failing.shown.exit).isNull()
        assertThat(failing.shown.message).isNull()
        assertThat(musics.musics.first()).hasSize(1)
    }

    @Test
    fun `a new music, its identity and its name survive the death of the process, in the saved state of the editor`() {
        // Given a new music, written and named, whose saved state the system kept
        val savedState = SavedStateHandle()
        val first = editor(savedState = savedState)
        first.onCellTouched(pitch("E4"), eighth = 0)
        first.onRenameAsked()
        first.onNameChanged("Fête")
        first.onRenameConfirmed()

        // When a new process makes a new editor on that state
        val restored = editor(savedState = savedState.restored())
        restored.onListen()

        // Then
        assertThat(restored.shown.editor.draft).isEqualTo(first.shown.editor.draft)
        assertThat(restored.shown.editor.status).isEqualTo(SaveStatus.NOT_SAVED)
        assertThat(restored.shown.name).isEqualTo("Fête")
        assertThat(music.playing).isEqualTo(NEW_ID)
    }

    @Test
    fun `a saved music changed is, after the death of the process, changed against its saved version`() = runTest {
        // Given a saved music, changed, whose saved state the system kept
        val saved = library.saveMusic("Joyeux 1", key = "joyeux")
        val savedState = SavedStateHandle()
        val first = editor(key = "joyeux", savedState = savedState)
        first.onCellTouched(pitch("G4"), eighth = 2)

        // When
        val restored = editor(key = "joyeux", savedState = savedState.restored())

        // Then
        assertThat(restored.shown.editor.draft).isEqualTo(first.shown.editor.draft)
        assertThat(restored.shown.editor.saved).isEqualTo(saved)
        assertThat(restored.shown.editor.status).isEqualTo(SaveStatus.MODIFIED)
        assertThat(restored.shown.name).isEqualTo("Joyeux 1")
    }

    @Test
    fun `a new music saved is, after the death of the process, the saved music of its identity`() = runTest {
        // Given a new music saved, whose saved state the system kept
        val savedState = SavedStateHandle()
        val first = editor(savedState = savedState)
        first.onCellTouched(pitch("E4"), eighth = 0)
        first.save(name = "Fête")

        // When
        val restored = editor(savedState = savedState.restored())

        // Then
        assertThat(restored.shown.editor.status).isEqualTo(SaveStatus.SAVED)
        assertThat(restored.shown.name).isEqualTo("Fête")
        assertThat(library.musics.first()).hasSize(1)
    }

    @Test
    fun `a draft in the saved state that cannot be read leaves the music as it was saved`() = runTest {
        // Given
        val saved = library.saveMusic("Joyeux 1", key = "joyeux")
        val savedState = SavedStateHandle()
        editor(key = "joyeux", savedState = savedState).onCellTouched(pitch("G4"), eighth = 2)
        savedState.keys().filter { it.endsWith("draft") }.forEach { savedState[it] = "garbage" }

        // When
        val restored = editor(key = "joyeux", savedState = savedState.restored())

        // Then
        assertThat(restored.shown.editor).isEqualTo(EditorState.open(saved))
    }

    /** The saved state as the system gives it back to a new process: its values, in a new handle. */
    private fun SavedStateHandle.restored(): SavedStateHandle = SavedStateHandle(keys().associateWith { get<Any>(it) })

    /** An editor on the music of [key], a new one when null, in [library], restored from [savedState]. */
    private fun editor(
        key: String? = null,
        library: MusicLibrary = this.library,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = composer(music, library, key, savedState)

    /** Saves the music through the window, under [name]. */
    private fun ComposerViewModel.save(name: String) {
        onSaveAsked()
        onNameChanged(name)
        onSaveConfirmed()
    }
}

/** Musics that cannot be written, as on a full disk: each change fails with an [IOException]. */
private class FailingLibrary(private val delegate: MusicLibrary = FakeMusicLibrary()) : MusicLibrary by delegate {
    override suspend fun save(music: SavedMusic) = throw IOException("The disk is full")

    override suspend fun rename(
        id: ScoreId.Personal,
        name: String,
    ) = throw IOException("The disk is full")

    override suspend fun delete(id: ScoreId.Personal): DeletedMusic = throw IOException("The disk is full")
}

/** Musics that save, but cannot choose the music played during the games: [setGameMusic] fails. */
private class GameMusicFailingLibrary(private val delegate: MusicLibrary) : MusicLibrary by delegate {
    override suspend fun setGameMusic(id: ScoreId.Personal?) = throw IOException("The disk is full")
}
