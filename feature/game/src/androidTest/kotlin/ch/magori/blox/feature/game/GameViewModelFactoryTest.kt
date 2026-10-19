package ch.magori.blox.feature.game

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
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.feature.game.navigation.GameRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The factory of the game view model with the real saved state of Android, which the JVM tests do not
 * have: what it takes from the creation extras of a navigation entry, and what it gives back after the
 * process died.
 */
internal class GameViewModelFactoryTest {
    private val seeds = mutableListOf<Long>()
    private val engineFactory = GameEngineFactory { seed ->
        seeds += seed
        ClassicGameEngineFactory.create(seed)
    }
    private val factory = silentGameViewModelFactory(engineFactory)
    private val route = GameRoute(startLevel = 3, seed = 42L)

    @Test
    @DisplayName("a new entry gives the game of its route, not paused")
    fun aNewEntryGivesTheGameOfItsRouteNotPaused() {
        // When
        val viewModel = EntryOwner(restored = null).gameViewModel(route)

        // Then
        assertThat(seeds).containsExactly(42L)
        assertThat(viewModel.uiState.value.paused).isFalse()
    }

    @Test
    @DisplayName("an entry restored after the process died gives the game of its route, paused")
    fun anEntryRestoredAfterTheProcessDiedGivesTheGameOfItsRoutePaused() {
        // Given an entry whose game started, then whose state the system saved before the process died
        val first = EntryOwner(restored = null)
        first.gameViewModel(route)
        val saved = first.save()

        // When the navigation restores the entry, in a new process
        val recreated = EntryOwner(restored = saved).gameViewModel(route)

        // Then
        assertThat(seeds).containsExactly(42L, 42L)
        assertThat(recreated.uiState.value.paused).isTrue()
    }

    /**
     * The owner of the view models of a navigation entry, as the decorator of Navigation 3 makes it: a
     * view model store and a saved state registry restored from [restored], which gives each view model
     * its `SavedStateHandle`.
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

        /** The game view model of [route], as the game entry asks for it (ADR 0013). */
        fun gameViewModel(route: GameRoute): GameViewModel {
            val extras = MutableCreationExtras().apply {
                set(SAVED_STATE_REGISTRY_OWNER_KEY, this@EntryOwner)
                set(VIEW_MODEL_STORE_OWNER_KEY, this@EntryOwner)
                set(GameViewModel.RouteKey, route)
            }
            return ViewModelProvider.create(viewModelStore, factory, extras)[GameViewModel::class]
        }

        /** What the system keeps of this entry when the application goes to the background. */
        fun save(): Bundle = Bundle().also(savedStateController::performSave)
    }
}
