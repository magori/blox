package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

/**
 * A [MusicPlayer] that plays only with the audio [focus], so that the music of the game never mixes
 * with that of another app. It asks for the focus when a score plays or resumes, and gives it back when
 * the score playing stops. Refused the focus (a phone call), it plays nothing: resuming the playback
 * later asks again.
 *
 * Holding the focus, it stops the music and gives the focus back when another app takes it for good
 * (a podcast) or when the headphones are unplugged: the game goes on in silence, and the music comes
 * back when the game is paused then resumed; the playback no longer plays ([Playback.isPlaying]), as
 * when the player it wraps stops by itself. Another app that takes the focus for a while (a voice
 * assistant) only pauses the music, which resumes where it stood once the focus is back; a score played
 * or resumed meanwhile waits for that return too. The system ducks the music by itself for a
 * notification or a navigation voice, without a word (Android 8 and later).
 *
 * A music that stops by itself (the player it wraps fails) does not give the focus back: the one who
 * follows [Playback.isPlaying] stops the playback, which gives it back (the home screen does). A game does
 * not follow it: its focus stays held, in silence, until the game is paused or over.
 *
 * Call it from the main thread, the one [focus] reports on, and follow [Playback.isPlaying] there too.
 */
public class FocusedMusicPlayer(private val player: MusicPlayer, private val focus: AudioFocus) : MusicPlayer {
    /** The playback that holds the focus, paused or not; null when the focus is not held. */
    private var current: FocusedPlayback? = null

    /** Whether the focus held is away for a while: the playback holding it waits for its return. */
    private var focusAway = false

    /**
     * The tempos prepared for the score played last: the player forgets them when it plays, which may come
     * later, once the focus is granted or back; they are prepared again then.
     */
    private var prepared: Set<Double> = emptySet()

    override fun play(score: Score): Playback {
        prepared = emptySet()
        return start { player.play(score).also { prepared.forEach(player::prepareTempo) } }
    }

    override fun setTempoFactor(factor: Double): Unit = player.setTempoFactor(factor)

    override fun prepareTempo(factor: Double) {
        prepared = prepared + factor
        player.prepareTempo(factor)
    }

    /**
     * Asks for the focus, unless it is held already by the playback [begin] replaces, then [begin]s
     * playing, or waits for the focus to come back if it is away for a while. Refused the focus, it plays
     * nothing; the playback it gives tries again when resumed.
     */
    private fun start(begin: () -> Playback): Playback {
        if (current == null && !focus.request(::onFocusEvent)) return Refused(begin)
        return FocusedPlayback(begin).also {
            current?.replaced()
            current = it
        }
    }

    private fun onFocusEvent(event: FocusEvent) {
        when (event) {
            FocusEvent.LOSS, FocusEvent.BECOMING_NOISY -> current?.stop()

            FocusEvent.TRANSIENT_LOSS -> {
                focusAway = true
                current?.pause()
            }

            FocusEvent.GAIN -> {
                focusAway = false
                current?.regain()
            }
        }
    }

    /**
     * A score played with the focus, from [begin]: at once, or once the focus is back. While it waits for
     * the focus, it plays ([Playback.isPlaying] is true): the music comes back by itself with the focus.
     * Its state is to be followed on the main thread, the one the focus reports on (`viewModelScope`):
     * the playback of [player] stopped by a pause is no longer followed once the state changed, so this
     * score never tells, for an instant, that it stopped.
     */
    private inner class FocusedPlayback(begin: () -> Playback) : Playback {
        /**
         * Tells whether this score plays: the state of the playback of [player] under way; [WAITING] while the
         * focus is away; [OVER] once stopped, replaced, or once the music stopped by itself.
         */
        private val state = MutableStateFlow(WAITING)

        /**
         * Whether this score is over for good: stopped, replaced, or its music stopped by itself, as seen by
         * whoever follows its state. It never plays again then, though the focus comes back.
         */
        private var over = false

        /** The first `false` followed, whatever its source, ends this score for good. */
        @OptIn(ExperimentalCoroutinesApi::class)
        override val isPlaying: Flow<Boolean> = state
            .flatMapLatest { source -> source.onEach { playing -> if (!playing) end() } }
            .distinctUntilChanged()

        /** How the score plays again: from where it started before it played, then from where it stood. */
        private var next: () -> Playback = begin

        /** The playback of [player] under way; null while the focus is away, or once stopped. */
        private var playing: Playback? = null

        init {
            if (!focusAway) play()
        }

        private fun play() {
            playing = next().also {
                next = it::resume
                state.value = it.isPlaying
            }
        }

        /** The focus is away for a while: the music stops, and waits for it, unless this score is over. */
        fun pause() = halt(if (over) OVER else WAITING)

        /**
         * Stops the playback of [player] under way, if any, once [newState] is told: the playback stopped is
         * no longer followed, and so never tells, for an instant, that this score stopped.
         */
        private fun halt(newState: Flow<Boolean>) {
            state.value = newState
            playing?.stop()
            playing = null
        }

        /** The focus is back: the music plays from where it paused; playing, or over, it stays as it was. */
        fun regain() {
            if (playing == null && !over) play()
        }

        // The focus is given back only by the playback holding it: one that was replaced gives nothing.
        override fun stop() {
            over = true
            halt(OVER)
            if (current !== this) return
            current = null
            focusAway = false
            focus.abandon()
        }

        override fun resume(): Playback = start(next)

        /** Another score plays in its place, with the focus it held: this one is over. */
        fun replaced() = end()

        /** This score is over for good: it never plays again. */
        private fun end() {
            over = true
            state.value = OVER
        }
    }

    /** A score refused the focus: it plays nothing, and asks again when resumed. Over from the start. */
    private inner class Refused(private val begin: () -> Playback) : Playback {
        override val isPlaying: Flow<Boolean> = OVER

        override fun stop() {}

        override fun resume(): Playback = start(begin)
    }

    private companion object {
        /** The state of a score waiting for the focus to come back: it plays again then, by itself. */
        val WAITING: Flow<Boolean> = flowOf(true)

        /** The state of a score stopped, replaced, refused the focus, or whose music stopped by itself. */
        val OVER: Flow<Boolean> = flowOf(false)
    }
}
