package ch.magori.blox.core.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A score listened to in a loop on a screen (the music editor, the musical garden): its [playback], watched in [scope]
 * once the screen shows it listened to ([watch]), so that a score that stops by itself (the audio focus lost for good
 * or refused, the output failing, the headphones unplugged; a score has no natural end) is told to
 * [onStoppedByItself], and the screen offers to listen again. [stop] stops it, and only it: a music started since
 * keeps playing; a score stopped this way is not told as stopped by itself.
 */
public class LoopListening(
    private val playback: Playback,
    private val scope: CoroutineScope,
    private val onStoppedByItself: () -> Unit,
) {
    private var watching: Job? = null

    /**
     * Starts watching the score. Called once the screen shows it listened to: a playback that stopped already (the
     * audio focus refused) is told at once, rather than before the listening began.
     */
    public fun watch() {
        if (watching != null) return
        watching = scope.launch {
            playback.isPlaying.first { !it }
            onStoppedByItself()
        }
    }

    /** Stops the score, no longer watched. */
    public fun stop() {
        watching?.cancel()
        playback.stop()
    }
}
