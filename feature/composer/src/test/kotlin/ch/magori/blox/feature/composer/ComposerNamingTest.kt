package ch.magori.blox.feature.composer

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.MODIFIED_AT
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.SaveStatus
import ch.magori.blox.feature.composer.model.pitch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.random.Random

/** The style of a music, chosen in the save window, and the name proposed for it (P4.27, P4.28). */
class ComposerNamingTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val library = FakeMusicLibrary()
    private val viewModel by lazy { composer(FakeMusicPlayer(), library) }

    @Test
    fun `Save proposes for a music written by hand the style Libre, and a name of its mode and tempo in its key`() {
        // Given a new music, in C major at 120 beats a minute
        viewModel.onCellTouched(pitch("E4"), eighth = 0)

        // When
        viewModel.onSaveAsked()

        // Then
        val window = viewModel.shown.dialog as ComposerDialog.Save
        assertThat(window.style).isNull()
        assertThat(window.name).isIn("Envolée en do", "Galop en do", "Cavalcade en do")
    }

    @Test
    fun `Save proposes for a music of the dice the category it was composed in, and a name of it in its key`() {
        // Given a music composed in Épique, in a natural minor key
        viewModel.onCategoryChosen(Category.EPIC)
        viewModel.onDice()

        // When
        viewModel.onSaveAsked()

        // Then
        val window = viewModel.shown.dialog as ComposerDialog.Save
        assertThat(window.style).isEqualTo(Category.EPIC)
        assertThat(window.name)
            .matches("(Bannière au vent|Les remparts|Chevauchée|Marche des géants) en (do|ré|fa|sol|la) mineur")
    }

    @Test
    fun `Rename proposes for a music never named a name of its style in its key`() {
        // When
        viewModel.onRenameAsked()

        // Then
        assertThat((viewModel.shown.dialog as ComposerDialog.Rename).name)
            .isIn("Envolée en do", "Galop en do", "Cavalcade en do")
    }

    @Test
    fun `the free name taken in place of a name another music has is kept whatever the style chosen`() = runTest {
        // Given
        library.saveMusic("Mon thème", key = "theme")
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()
        viewModel.onNameChanged("Mon thème")

        // When
        viewModel.onFreeNameTaken()
        viewModel.onStyleChosen(Category.RETRO)

        // Then
        assertThat((viewModel.shown.dialog as ComposerDialog.Save).name).isEqualTo("Mon thème 2")
    }

    @Test
    fun `a saved music opens the window on its own style, and keeps its name whatever the style chosen`() = runTest {
        // Given a music saved in Joyeux
        val id = ScoreId.Personal("party")
        val draft = EditorState.new().touch(pitch("C5"), eighth = 0).draft
        library.save(SavedMusic(id, "Fête", Category.JOYFUL, draft.scale, draft.toScore(id)))
        val saved = composer(FakeMusicPlayer(), library, key = "party")

        // When
        saved.onSaveAsked()
        val opened = saved.shown.dialog as ComposerDialog.Save
        saved.onStyleChosen(Category.MYSTERIOUS)

        // Then
        assertThat(opened.style).isEqualTo(Category.JOYFUL)
        assertThat(opened.name).isEqualTo("Fête")
        assertThat(saved.shown.dialog).isEqualTo(
            ComposerDialog.Save(name = "Fête", forGames = false, style = Category.MYSTERIOUS, nameChosen = true),
        )
    }

    @Test
    fun `a style chosen in the window proposes a name of that style, until the player types a name of their own`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()

        // When
        viewModel.onStyleChosen(Category.CALM)

        // Then
        val calm = viewModel.shown.dialog as ComposerDialog.Save
        assertThat(calm.style).isEqualTo(Category.CALM)
        assertThat(calm.name)
            .isIn("Brume du lac en do", "Jardin du soir en do", "Sieste au verger en do", "Ciel de lait en do")

        // When
        viewModel.onNameChanged("Mon thème")
        viewModel.onStyleChosen(null)

        // Then
        assertThat(viewModel.shown.dialog)
            .isEqualTo(ComposerDialog.Save(name = "Mon thème", forGames = false, style = null, nameChosen = true))
    }

    @Test
    fun `the music is saved in the style chosen, the editor going on in that style, its changes counted from there`() =
        runTest {
            // Given
            viewModel.onCellTouched(pitch("E4"), eighth = 0)
            viewModel.onSaveAsked()
            viewModel.onStyleChosen(Category.MELANCHOLIC)
            val name = (viewModel.shown.dialog as ComposerDialog.Save).name

            // When
            viewModel.onSaveConfirmed()

            // Then
            val draft = viewModel.shown.editor.draft
            assertThat(draft.category).isEqualTo(Category.MELANCHOLIC)
            assertThat(library.musics.first())
                .containsExactly(
                    SavedMusic(NEW_ID, name, Category.MELANCHOLIC, draft.scale, draft.toScore(NEW_ID), MODIFIED_AT),
                )
            assertThat(viewModel.shown.editor.status).isEqualTo(SaveStatus.SAVED)
        }

    @Test
    fun `a music saved in a style and saved again in Libre is saved without a category`() = runTest {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()
        viewModel.onStyleChosen(Category.DANCE)
        viewModel.onSaveConfirmed()

        // When
        viewModel.onSaveAsked()
        viewModel.onStyleChosen(null)
        viewModel.onSaveConfirmed()

        // Then
        assertThat(library.musics.first().single().category).isNull()
        assertThat(viewModel.shown.editor.draft.category).isNull()
    }

    @Test
    fun `a music of the dice saved in Libre is saved without a category`() = runTest {
        // Given a music composed in Calme
        viewModel.onCategoryChosen(Category.CALM)
        viewModel.onDice()
        viewModel.onSaveAsked()

        // When
        viewModel.onStyleChosen(null)
        viewModel.onSaveConfirmed()

        // Then
        assertThat(library.musics.first().single().category).isNull()
        assertThat(viewModel.shown.editor.draft.category).isNull()
    }

    @Test
    fun `a new music renamed keeps its name in the save window whatever the style chosen`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onRenameAsked()
        viewModel.onNameChanged("Mon thème")
        viewModel.onRenameConfirmed()

        // When
        viewModel.onSaveAsked()
        viewModel.onStyleChosen(Category.EPIC)

        // Then
        assertThat((viewModel.shown.dialog as ComposerDialog.Save).name).isEqualTo("Mon thème")
    }

    @Test
    fun `the window closed and opened again proposes the same name, as long as the style is the same`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onSaveAsked()
        viewModel.onStyleChosen(Category.JOYFUL)
        val proposed = (viewModel.shown.dialog as ComposerDialog.Save).name

        // When
        viewModel.onDialogDismissed()
        viewModel.onSaveAsked()
        viewModel.onStyleChosen(Category.JOYFUL)

        // Then
        assertThat((viewModel.shown.dialog as ComposerDialog.Save).name).isEqualTo(proposed)
    }

    @Test
    fun `the names are drawn with their own random, injected like the one of the dice`() {
        // Given two editors whose dice draw alike, but not their names
        val first = composer(FakeMusicPlayer(), library, names = FirstRandom)
        val last = composer(FakeMusicPlayer(), library, names = LastRandom)

        // When
        first.onSaveAsked()
        last.onSaveAsked()

        // Then
        assertThat((first.shown.dialog as ComposerDialog.Save).name).isEqualTo("Envolée en do")
        assertThat((last.shown.dialog as ComposerDialog.Save).name).isEqualTo("Cavalcade en do")
    }

    private companion object {
        /** A random that always draws the first of the choices. */
        val FirstRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        /** A random that always draws the last of the choices. */
        val LastRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0

            override fun nextInt(
                from: Int,
                until: Int,
            ): Int = until - 1
        }
    }
}
