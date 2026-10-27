package ch.magori.blox.feature.composer

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.navigation.ComposerRoute
import ch.magori.blox.feature.composer.navigation.composerEntry
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.random.Random

/**
 * The composer entry in a navigation display decorated as `app` decorates it: what its route hands to the factory of
 * the view model, through the creation extras (ADR 0013), the default extras of the entry kept beside it.
 */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ComposerEntryTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val library = FakeMusicLibrary()

    @Test
    @DisplayName("the entry of a saved music opens the editor on that music")
    fun theEntryOfASavedMusicOpensThatMusic() = extension.use {
        // Given a music saved under the key "joyeux"
        val draft = EditorState.new().touch(Pitch.parse("C5"), eighth = 0).draft
        val id = ScoreId.Personal("joyeux")
        runBlocking { library.save(SavedMusic(id, "Joyeux 1", null, draft.scale, draft.toScore(id))) }

        // When its entry is shown
        showEntry(ComposerRoute(musicKey = "joyeux"))

        // Then the editor opens on it, under its name, as it was saved: its one note, no dice rolled over it
        onNodeWithText("Joyeux 1").assertExists()
        val notes = onAllNodes(isNote).fetchSemanticsNodes().map { it.config[SemanticsProperties.ContentDescription] }
        assertThat(notes).containsExactly(listOf("Do haut, temps 1"))
    }

    @Test
    @DisplayName("the entry of Au hasard opens the editor on a music rolled at once")
    fun theEntryOfAuHasardRollsTheDice() = extension.use {
        // When the entry of Au hasard is shown
        showEntry(ComposerRoute(rollDice = true))

        // Then the dice has rolled: the new music holds notes already
        assertThat(onAllNodes(isNote).fetchSemanticsNodes()).isNotEmpty()
    }

    /**
     * Shows [route] alone on a stack with the decorators of `app`, in their order, its factory composing on the main
     * thread, at once.
     */
    private fun ComposeContext.showEntry(route: ComposerRoute) {
        val factory = ComposerViewModel.Factory(FakeMusicPlayer(), library, Random(SEED), Dispatchers.Main.immediate)
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                NavDisplay(
                    backStack = listOf<NavKey>(route),
                    onBack = {},
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        composerEntry(onBack = {}, onDeleted = {}, viewModelFactory = factory)
                    },
                )
            }
        }
        waitForIdle()
    }

    private companion object {
        /** The first seed of the draws of the dice. */
        const val SEED = 42L

        /** A cell of the grid that holds a note. */
        val isNote = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "note")
    }
}
