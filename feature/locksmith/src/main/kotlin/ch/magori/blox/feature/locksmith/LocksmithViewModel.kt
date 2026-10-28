package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.SoundCue
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.audio.playCues
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockUpdate
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.locksmith.sound.soundCuesFor
import ch.magori.blox.feature.locksmith.sound.warmUpEffectsOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * The game of one [LocksmithRoute]: its padlock, generated from the seed of the route (ADR 0014, 0025), played by the
 * actions of the player, which the domain judges. The only writer of the state of the screen. It plays the sound
 * effects of the judged actions (the chords, the opening flourish, the refusal), warmed up at the opening, unless the
 * settings turn them off. A rank that is not given is a programming error of the screen, refused by the domain.
 */
class LocksmithViewModel internal constructor(
    route: LocksmithRoute,
    private val soundEffects: SoundEffectPlayer,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    private val state = MutableStateFlow(
        LocksmithUiState(level = route.level, game = PadlockGame.start(Padlock.generate(route.level, route.seed))),
    )

    /** What the screen shows. */
    internal val uiState: StateFlow<LocksmithUiState> = state.asStateFlow()

    /** Whether the padlock plays its sound effects, from the settings; false until they answer, as in the game. */
    private var soundEffectsOn = false

    init {
        // Read once, at the opening of the screen, as the game does. Unreadable settings give the defaults (the contract
        // of the repository).
        viewModelScope.launch {
            soundEffectsOn = preferences.preferences.first().soundEffects
            if (soundEffectsOn) soundEffects.warmUp(warmUpEffectsOf(state.value.game))
        }
    }

    /** Selects the free given piece of [rank]. */
    internal fun select(rank: Int): Unit = arrange { it.copy(game = it.game.select(rank)) }

    /** Turns the selected piece a quarter turn by [rotation]. */
    internal fun turn(rotation: Rotation): Unit = arrange { it.copy(game = it.game.turn(rotation), turned = true) }

    /**
     * A touch of the padlock on [position]: on the preview, places it (refused over a placed piece, which the preview
     * hides); on a placed piece, takes it back as a preview where it lay; elsewhere, aims the selected piece there. The
     * ring of metal is not touched: the pivot of the preview stays inside it.
     */
    internal fun tap(position: Position) {
        if (!position.isInside) return
        val current = state.value
        val rank = current.game.rankAt(position)
        when {
            current.preview?.cells?.contains(position) == true -> placeAim()
            rank != null -> takeBack(rank)
            else -> arrange { it.copy(aim = position) }
        }
    }

    /** Places the preview, if there is one; it is gone once placed, kept where it is when refused. */
    internal fun placeAim() {
        state.value.aim?.let(::place)
    }

    /** Places the selected piece, its pivot on [pivot]. */
    internal fun place(pivot: Position): Unit = judge { it.place(pivot) }

    /** Takes the placed piece of [rank] back: it becomes the preview, where it lay. */
    internal fun takeBack(rank: Int) {
        val pivot = state.value.game.placed[rank]?.pivot ?: return
        judge(aimAfter = pivot) { it.takeBack(rank) }
    }

    /** Asks for a hint, shown until the next placement accepted. */
    internal fun hint(): Unit = judge { it.hint() }

    /** Starts the same padlock over: nothing placed, no hint. */
    internal fun restart(): Unit = state.update {
        it.copy(game = it.game.restart(), events = emptyList(), hint = null, aim = null, turned = false)
    }

    /**
     * An action of the player that the domain does not judge (choose, turn, aim): the events of the last judged action
     * are forgotten, a refusal told no longer once the preview has changed, without counting a move. Nothing changes once
     * the padlock is open.
     */
    private fun arrange(action: (LocksmithUiState) -> LocksmithUiState) = state.update { current ->
        if (current.game.isOpen) current else action(current.copy(turned = false)).copy(events = emptyList())
    }

    /**
     * An action the domain judges, in one update: the next game, the events it tells, and the hint they leave shown;
     * the preview is gone once a piece is placed, or put on [aimAfter]. An action the domain ignores (it tells no event)
     * changes nothing, the events of the last one kept.
     */
    private fun judge(
        aimAfter: Position? = null,
        action: (PadlockGame) -> PadlockUpdate,
    ) {
        val current = state.value
        val update = action(current.game)
        val (game, events) = update
        if (events.isEmpty()) return
        val hint = when (val first = events.first()) {
            is PadlockEvent.HintGiven -> first.hint
            is PadlockEvent.PiecePlaced, is PadlockEvent.PieceTakenBack -> null
            else -> current.hint
        }
        val aim = when {
            aimAfter != null -> aimAfter
            events.any { it is PadlockEvent.PiecePlaced } -> null
            else -> current.aim
        }
        state.value =
            current.copy(game = game, events = events, move = current.move + 1, hint = hint, aim = aim, turned = false)
        play(soundCuesFor(update))
    }

    /**
     * Plays [cues], at once or once their wait is over: an action on the screen meanwhile cuts no sound, but a cue still
     * waiting dies with the screen left, so that it is not heard over the next one.
     */
    private fun play(cues: List<SoundCue>) {
        if (!soundEffectsOn || cues.isEmpty()) return
        viewModelScope.launch { soundEffects.playCues(cues) }
    }

    /**
     * Builds the view model of an entry of the Locksmith (ADR 0013). The route of the entry comes in the
     * creation extras under `RouteKey`, since only the navigation entry knows it; the player of the sound effects and the
     * settings come from the application, which owns them (ADR 0007).
     */
    class Factory(private val soundEffects: SoundEffectPlayer, private val preferences: UserPreferencesRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            val route =
                requireNotNull(extras[RouteKey]) { "The route of the padlock is missing from the creation extras" }
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return LocksmithViewModel(route, soundEffects, preferences) as T
        }
    }

    internal companion object {
        /** The key of the route of the entry in the creation extras. */
        val RouteKey: CreationExtras.Key<LocksmithRoute> = object : CreationExtras.Key<LocksmithRoute> {}
    }
}
