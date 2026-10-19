package ch.magori.blox.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.game.navigation.gameEntry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The game entry in a navigation display set up as `app` sets it up, through a save and restore of its
 * state: what the navigation does when the system restores the application after its process died.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameEntryRestorationTest {
    @Test
    @DisplayName("a game entry restored after the process died shows its game paused")
    fun aGameEntryRestoredAfterTheProcessDiedShowsItsGamePaused() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way, on a stack restorable as the application's
            val resume = checkNotNull(activity).getString(R.string.feature_game_pause_resume)
            val restoration = StateRestorationTester(this)
            restoration.setContent { GameStack() }
            onNodeWithText(resume).assertDoesNotExist()

            // When its state is saved, the process dies with its view models, and the state is restored
            restoration.emulateSaveAndRestore()

            // Then the game waits for the player, paused
            onNodeWithText(resume).assertIsDisplayed()
        }
    }
}

/**
 * A stack holding one game, with the decorators of `app`, in their order. Its view models live in the
 * store of [ProcessViewModels], which a restoration does not keep: it is remembered, not saved, so the
 * restored composition gets a new one, as a new process would.
 */
@Composable
private fun GameStack() {
    val process = remember { ProcessViewModels() }
    // The process dies: its view models with it.
    DisposableEffect(process) { onDispose { process.viewModelStore.clear() } }
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides process,
        LocalLifecycleOwner provides process,
    ) {
        BloxTheme(kind = BloxThemeKind.ORIGINAL) {
            NavDisplay(
                backStack = listOf<NavKey>(GameRoute(startLevel = 0, seed = 1L)),
                onBack = {},
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    gameEntry(onBack = {}, onReplay = {}, viewModelFactory = silentGameViewModelFactory())
                },
            )
        }
    }
}

/**
 * The view models of one process of the application, and its lifecycle, held at CREATED: the screen draws no
 * frame (it does from STARTED on, ADR 0012), which would keep Compose busy for ever and the test from ever
 * finding it idle. No ON_PAUSE comes either: only the restoration can pause the game.
 */
private class ProcessViewModels :
    ViewModelStoreOwner,
    LifecycleOwner {
    override val viewModelStore = ViewModelStore()
    override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.CREATED }
}
