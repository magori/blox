package ch.magori.blox.feature.composer

import androidx.lifecycle.SavedStateHandle
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.CategoryChoice
import ch.magori.blox.core.music.MusicGenerator
import ch.magori.blox.core.music.Surprise
import ch.magori.blox.feature.composer.model.SaveStatus
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.pitch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.random.Random

/** *Au hasard*: the category chosen, the music the generator composes in place of the one being edited, and its undo. */
class ComposerDiceTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val music = FakeMusicPlayer()
    private val library = FakeMusicLibrary()
    private val viewModel by lazy { composer(music, library) }

    @Test
    fun `the dice is ready for Surprise at first, and a category touched is chosen in its place`() {
        // Then
        assertThat(viewModel.shown.choice).isEqualTo(Surprise)

        // When
        viewModel.onCategoryChosen(Category.CALM)

        // Then
        assertThat(viewModel.shown.choice).isEqualTo(Category.CALM)
    }

    @Test
    fun `the dice opens the music the generator composes in the category chosen, from a seed drawn at each roll`() {
        // Given
        val seeds = Random(DICE_SEED)
        viewModel.onCategoryChosen(Category.CALM)

        // When
        viewModel.onDice()

        // Then
        assertThat(viewModel.shown.editor.draft).isEqualTo(generated(Category.CALM, seeds.nextLong()))

        // When
        viewModel.onCategoryChosen(Surprise)
        viewModel.onDice()

        // Then
        assertThat(viewModel.shown.editor.draft).isEqualTo(generated(Surprise, seeds.nextLong()))
    }

    @Test
    fun `the dice over notes can be undone, once, and a message says so`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        val written = viewModel.shown.editor.draft

        // When
        viewModel.onDice()

        // Then
        assertThat(viewModel.shown.editor.canUndo).isTrue()
        assertThat(viewModel.shown.message).isEqualTo(ComposerMessage.DICE_ROLLED)

        // When
        viewModel.onUndo()

        // Then
        assertThat(viewModel.shown.editor.draft).isEqualTo(written)
        assertThat(viewModel.shown.editor.canUndo).isFalse()
    }

    @Test
    fun `the dice over an empty grid keeps no undo, and no message says there is one`() {
        // When
        viewModel.onDice()

        // Then
        assertThat(viewModel.shown.editor.canUndo).isFalse()
        assertThat(viewModel.shown.message).isNull()
    }

    @Test
    fun `the dice composes off the main thread, and a second roll meanwhile does nothing`() {
        // Given an editor that composes on a dispatcher of its own, which runs only when told to
        val composing = StandardTestDispatcher()
        val editor = composer(music, library, generation = composing)
        val seeds = Random(DICE_SEED)

        // When
        editor.onDice()
        editor.onDice()

        // Then nothing is composed until the dispatcher runs, and then a single music
        assertThat(editor.shown.editor.draft.isEmpty).isTrue()
        composing.scheduler.advanceUntilIdle()
        assertThat(editor.shown.editor.draft).isEqualTo(generated(Surprise, seeds.nextLong()))

        // When the roll is done
        editor.onDice()
        composing.scheduler.advanceUntilIdle()

        // Then the dice rolls again, from the next seed
        assertThat(editor.shown.editor.draft).isEqualTo(generated(Surprise, seeds.nextLong()))
    }

    @Test
    fun `a note touched while the dice composes is replaced by the music rolled, and Annuler brings it back`() {
        // Given a note, and the dice composing
        val composing = StandardTestDispatcher()
        val editor = composer(music, library, generation = composing)
        editor.onCellTouched(pitch("E4"), eighth = 0)
        editor.onDice()

        // When a note is touched meanwhile
        editor.onCellTouched(pitch("G4"), eighth = 2)
        val touched = editor.shown.editor.draft
        composing.scheduler.advanceUntilIdle()

        // Then
        assertThat(editor.shown.editor.draft).isEqualTo(generated(Surprise, Random(DICE_SEED).nextLong()))

        // When
        editor.onUndo()

        // Then
        assertThat(editor.shown.editor.draft).isEqualTo(touched)
    }

    @Test
    fun `the music rolled is left aside when a window opened while the dice composed`() {
        // Given a note, and the dice composing
        val composing = StandardTestDispatcher()
        val editor = composer(music, library, generation = composing)
        editor.onCellTouched(pitch("E4"), eighth = 0)
        val written = editor.shown.editor.draft
        editor.onDice()

        // When a window opens meanwhile
        editor.onRenameAsked(base = "Ma musique")
        composing.scheduler.advanceUntilIdle()

        // Then
        assertThat(editor.shown.editor.draft).isEqualTo(written)
        assertThat(editor.shown.dialog).isInstanceOf(ComposerDialog.Rename::class.java)
    }

    @Test
    fun `the music rolled is left aside when the music was saved while the dice composed`() = runTest {
        // Given a note, the dice composing, and musics that take their time to write
        val composing = StandardTestDispatcher()
        val slow = SlowLibrary(library)
        val editor = composer(music, slow, generation = composing)
        editor.onCellTouched(pitch("E4"), eighth = 0)
        val written = editor.shown.editor.draft
        editor.onDice()

        // When the music is saved meanwhile, the window closed once it is written
        editor.onSaveAsked(base = "Ma musique")
        editor.onSaveConfirmed()
        slow.gate.complete(Unit)
        composing.scheduler.advanceUntilIdle()

        // Then
        assertThat(editor.shown.dialog).isNull()
        assertThat(editor.shown.editor.draft).isEqualTo(written)
        assertThat(editor.shown.editor.status).isEqualTo(SaveStatus.SAVED)
    }

    @Test
    fun `the dice does not roll while a window is open`() {
        // Given a note, and a window open
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        val written = viewModel.shown.editor.draft
        viewModel.onRenameAsked(base = "Ma musique")

        // When
        viewModel.onDice()
        viewModel.onDialogDismissed()

        // Then
        assertThat(viewModel.shown.editor.draft).isEqualTo(written)
    }

    @Test
    fun `the dice over a music emptied by Effacer keeps no undo, the clearing lost as the specification wants`() {
        // Given the only note cleared, which could be undone
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onClearBar()

        // When
        viewModel.onDice()

        // Then
        assertThat(viewModel.shown.editor.canUndo).isFalse()
        assertThat(viewModel.shown.message).isNull()
    }

    @Test
    fun `an editor opened for the dice, as Au hasard of my musics does, opens a new music it rolled, without undo`() {
        // When
        val editor = composer(music, library, rollDice = true)

        // Then
        val shown = editor.shown
        assertThat(shown.editor.draft).isEqualTo(generated(Surprise, Random(DICE_SEED).nextLong()))
        assertThat(shown.editor.saved).isNull()
        assertThat(shown.editor.canUndo).isFalse()
        assertThat(shown.message).isNull()
    }

    @Test
    fun `an editor opened for the dice, restored after the death of the process, keeps its music and rolls no more`() {
        // Given the music rolled, then changed, and the process dead
        val savedState = SavedStateHandle()
        val first = composer(music, library, savedState = savedState, rollDice = true)
        first.onCellTouched(pitch("E4"), eighth = 0)
        val written = first.shown.editor.draft

        // When the navigation opens the entry again, still for the dice, with what it saved
        val restored = composer(
            music,
            library,
            savedState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) }),
            random = Random(DICE_SEED + 1),
            rollDice = true,
        )

        // Then
        assertThat(restored.shown.editor.draft).isEqualTo(written)
    }

    /** The draft of the music the generator composes in [choice] from [seed]. */
    private fun generated(
        choice: CategoryChoice,
        seed: Long,
    ): ScoreDraft {
        val music = MusicGenerator.generate(choice, seed, NEW_ID)
        return checkNotNull(ScoreDraft.fromScore(music.score, music.scale, music.category))
    }
}
