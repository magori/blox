package ch.magori.blox.feature.composer

import android.os.Bundle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The factory that `app` builds, with the real saved state of Android, which the JVM tests do not have: what it takes
 * from the creation extras of a navigation entry, and what it gives back after the process died.
 */
internal class ComposerViewModelFactoryTest {
    private val library = FakeMusicLibrary()
    private val factory = ComposerViewModel.Factory(FakeMusicPlayer(), library)

    @Test
    @DisplayName("an entry without a music gives an editor on a new music")
    fun anEntryWithoutAMusicGivesANewMusic() {
        // When
        val editor = onMain { EntryOwner(restored = null).composer(musicKey = null) }

        // Then
        assertThat(editor.uiState.value?.editor).isEqualTo(EditorState.new())
    }

    @Test
    @DisplayName("an entry of a saved music gives an editor on that music, as it was saved")
    fun anEntryOfASavedMusicGivesThatMusic() {
        // Given
        val draft = EditorState.new().touch(Pitch.parse("C5"), eighth = 0).draft
        val id = ScoreId.Personal("joyeux")
        runBlocking { library.save(SavedMusic(id, "Joyeux 1", null, draft.scale, draft.toScore(id))) }

        // When
        val editor = onMain { EntryOwner(restored = null).composer(musicKey = "joyeux") }

        // Then
        assertThat(editor.uiState.value?.editor).isEqualTo(EditorState.open(draft))
        assertThat(editor.uiState.value?.name).isEqualTo("Joyeux 1")
    }

    @Test
    @DisplayName("an entry restored after the process died gives back the music being written")
    fun anEntryRestoredAfterTheProcessDiedGivesBackTheMusic() {
        // Given a new music written, whose entry the system saved before the process died
        val first = onMain { EntryOwner(restored = null) }
        val written =
            onMain { first.composer(musicKey = null).also { it.onCellTouched(Pitch.parse("E4"), eighth = 0) } }
        val saved = onMain { first.save() }

        // When the navigation restores the entry, in a new process
        val restored = onMain { EntryOwner(restored = saved).composer(musicKey = null) }

        // Then
        assertThat(restored.uiState.value?.editor?.draft).isEqualTo(written.uiState.value?.editor?.draft)
    }

    /**
     * Runs [block] on the main thread, as the navigation does: the view model collects on `Dispatchers.Main.immediate`,
     * at once from there.
     */
    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    /**
     * The owner of the view models of a navigation entry, as the decorator of Navigation 3 makes it: a view model store
     * and a saved state registry restored from [restored], which gives each view model its `SavedStateHandle`.
     */
    private inner class EntryOwner(restored: Bundle?) :
        ViewModelStoreOwner,
        SavedStateRegistryOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
        override val viewModelStore = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        init {
            savedStateController.performAttach()
            savedStateController.performRestore(restored)
            enableSavedStateHandles()
        }

        /** The editor of the music of [musicKey], as the composer entry asks for it (ADR 0013). */
        fun composer(musicKey: String?): ComposerViewModel {
            val extras = MutableCreationExtras().apply {
                set(SAVED_STATE_REGISTRY_OWNER_KEY, this@EntryOwner)
                set(VIEW_MODEL_STORE_OWNER_KEY, this@EntryOwner)
                musicKey?.let { set(ComposerViewModel.MusicKey, it) }
            }
            return ViewModelProvider.create(viewModelStore, factory, extras)[ComposerViewModel::class]
        }

        /** What the system keeps of this entry when the application goes to the background. */
        fun save(): Bundle = Bundle().also(savedStateController::performSave)
    }
}
