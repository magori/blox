package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score

/**
 * A [MusicPlayer] that plays only with the audio [focus], so that the music of the game never mixes
 * with that of another app. It asks for the focus when a score plays or resumes, and gives it back when
 * the score playing stops. Refused the focus (a phone call), it plays nothing: resuming the playback
 * later asks again.
 *
 * Holding the focus, it stops the music and gives the focus back when another app takes it for good
 * (a podcast) or when the headphones are unplugged: the game goes on in silence, and the music comes
 * back when the game is paused then resumed. Another app that takes the focus for a while (a voice
 * assistant) only pauses the music, which resumes where it stood once the focus is back; a score played
 * or resumed meanwhile waits for that return too. The system ducks the music by itself for a
 * notification or a navigation voice, without a word (Android 8 and later).
 *
 * Call it from the main thread, the one [focus] reports on.
 */
public class FocusedMusicPlayer(private val player: MusicPlayer, private val focus: AudioFocus) : MusicPlayer {
    /** The playback that holds the focus, paused or not; null when the focus is not held. */
    private var current: FocusedPlayback? = null

    /** Whether the focus held is away for a while: the playback holding it waits for its return. */
    private var focusAway = false

    override fun play(score: Score): Playback = start { player.play(score) }

    override fun setTempoFactor(factor: Double): Unit = player.setTempoFactor(factor)

    /**
     * Asks for the focus, unless it is held already by the playback [begin] replaces, then [begin]s
     * playing, or waits for the focus to come back if it is away for a while. Refused the focus, it plays
     * nothing; the playback it gives tries again when resumed.
     */
    private fun start(begin: () -> Playback): Playback {
        if (current == null && !focus.request(::onFocusEvent)) return Refused(begin)
        return FocusedPlayback(begin).also { current = it }
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

    /** A score played with the focus, from [begin]: at once, or once the focus is back. */
    private inner class FocusedPlayback(begin: () -> Playback) : Playback {
        /** How the score plays again: from where it started before it played, then from where it stood. */
        private var next: () -> Playback = begin

        /** The playback of [player] under way; null while the focus is away, or once stopped. */
        private var playing: Playback? = null

        init {
            if (!focusAway) play()
        }

        private fun play() {
            playing = next().also { next = it::resume }
        }

        fun pause() {
            playing?.stop()
            playing = null
        }

        /** The focus is back: the music plays from where it paused; playing, it goes on as it was. */
        fun regain() {
            if (playing == null) play()
        }

        // The focus is given back only by the playback holding it: one that was replaced gives nothing.
        override fun stop() {
            pause()
            if (current !== this) return
            current = null
            focusAway = false
            focus.abandon()
        }

        override fun resume(): Playback = start(next)
    }

    /** A score refused the focus: it plays nothing, and asks again when resumed. */
    private inner class Refused(private val begin: () -> Playback) : Playback {
        override fun stop() {}

        override fun resume(): Playback = start(begin)
    }
}
