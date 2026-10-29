package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.SoundCue
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.audio.playCues
import ch.magori.blox.core.data.PadlockProgressRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.reflect.KClass

/**
 * The game of one [LocksmithRoute]: its padlock, that of the level of the route, the same for everyone (ADR 0014, 0025), played by the
 * actions of the player, which the domain judges. The only writer of the state of the screen. It plays the sound
 * effects of the judged actions (the chords, the opening flourish, the refusal), warmed up at the opening, unless the
 * settings turn them off. A rank that is not given is a programming error of the screen, refused by the domain.
 *
 * The opening of the padlock is written to the [progress] of the Serrurier in [appScope], the scope of the application,
 * which outlives the screen: leaving it at once does not lose the opening.
 */
class LocksmithViewModel internal constructor(
    route: LocksmithRoute,
    private val soundEffects: SoundEffectPlayer,
    preferences: UserPreferencesRepository,
    private val progress: PadlockProgressRepository,
    private val appScope: CoroutineScope,
    private val cleared: ClearedProgressRelay,
) : ViewModel() {
    private val state = MutableStateFlow(
        LocksmithUiState(level = route.level, game = PadlockGame.start(Padlock.ofLevel(route.level))),
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
        // Followed while the screen lives: the best opening of the padlock and the frontier, for the card. A progress that
        // cannot be read gives an empty one, once, and the flow ends (the contract of the repository): the screen does not
        // read it again, only each opening, once, to write it.
        viewModelScope.launch {
            progress.progress.collect { kept ->
                state.update {
                    it.copy(best = kept.openings.getOrNull(it.level - 1)?.toResult(), frontier = kept.frontier)
                }
            }
        }
    }

    /**
     * Tells that the progress was just cleared on the screen of the padlocks, if it was and no other padlock told it: the
     * padlock starts over, as one never opened (its view model may be kept, the key of padlock 1 under the screen of the
     * padlocks being equal to the one put back), and the message says so until the first action. Called once the screen
     * shows, so that TalkBack hears the message change.
     */
    internal fun showClearedProgress() {
        if (!cleared.take()) return
        restart()
        state.update { it.copy(cleared = true) }
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

    /** Starts the same padlock over: nothing placed, no hint, nothing told of the opening before. */
    internal fun restart(): Unit = state.update {
        it.copy(
            game = it.game.restart(),
            events = emptyList(),
            hint = null,
            aim = null,
            turned = false,
            unrecorded = false,
            outcome = null,
            cleared = false,
        )
    }

    /**
     * An action of the player that the domain does not judge (choose, turn, aim): the events of the last judged action
     * are forgotten, a refusal told no longer once the preview has changed, without counting a move. Nothing changes once
     * the padlock is open.
     */
    private fun arrange(action: (LocksmithUiState) -> LocksmithUiState) = state.update { current ->
        if (current.game.isOpen) {
            current
        } else {
            action(
                current.copy(turned = false, cleared = false),
            ).copy(events = emptyList())
        }
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
        state.value = current.copy(
            game = game,
            events = events,
            move = current.move + 1,
            hint = hint,
            aim = aim,
            turned = false,
            cleared = false,
        )
        play(soundCuesFor(update))
        if (game.isOpen) record(game)
    }

    /**
     * Writes the opening of the [opened] game to the progress, in the scope of the application. The padlocks open in
     * order: one beyond the frontier, as after an opening that could not be written, is not recorded. The frontier is
     * read when the opening is written, not the one the screen followed, which may be behind a write still under way or
     * ahead of a progress cleared since. The opening has as many pieces as the best of its padlock, the precondition of
     * [PadlockProgressRepository.record]: padlock n is the same for everyone and for ever (ADR 0014). An opening that
     * cannot be written (an [IOException], a full disk or a progress of a later version) is not recorded either: the game
     * goes on as it is, the screen telling it, as long as the padlock is not started over. Any other failure is a bug,
     * and goes through to [appScope].
     */
    private fun record(opened: PadlockGame) {
        val level = state.value.level
        val opening = checkNotNull(opened.result).toOpening()
        appScope.launch {
            val outcome = try {
                if (level <= progress.progress.first().frontier) progress.record(level, opening) else null
            } catch (_: IOException) {
                null
            }
            state.update { if (it.game == opened) it.copy(outcome = outcome, unrecorded = outcome == null) else it }
        }
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
     * settings come from the application, which owns them (ADR 0007), as the [progress] of the Serrurier and [appScope],
     * where it is written, for the whole life of the process, and the relay of a progress [cleared] on the screen of the
     * padlocks, one for the process.
     */
    class Factory(
        private val soundEffects: SoundEffectPlayer,
        private val preferences: UserPreferencesRepository,
        private val progress: PadlockProgressRepository,
        private val appScope: CoroutineScope,
        private val cleared: ClearedProgressRelay,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            val route =
                requireNotNull(extras[RouteKey]) { "The route of the padlock is missing from the creation extras" }
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return LocksmithViewModel(route, soundEffects, preferences, progress, appScope, cleared) as T
        }
    }

    internal companion object {
        /** The key of the route of the entry in the creation extras. */
        val RouteKey: CreationExtras.Key<LocksmithRoute> = object : CreationExtras.Key<LocksmithRoute> {}
    }
}
