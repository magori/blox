package ch.magori.blox.core.audio

import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.flow.Flow

/**
 * Plays a music file of the player in a loop, at its own speed and at the volume set, one file at a time.
 * [AudioFilePlayer] is the player of the app; [MusicSwitch] plays it in turn with the scores, and falls back on a
 * score when a file cannot be played.
 */
public interface MusicFilePlayer : VolumeControl {
    /**
     * Plays [file] in a loop from its start, in place of the file playing, if any; null if it cannot be opened (its
     * permission lost, the file deleted), and the file playing, if any, goes on. A file that opens but fails before
     * it starts playing (a format the device cannot decode) tells [onUnplayable], on the main thread, then ends.
     */
    public fun play(
        file: MusicFile,
        onUnplayable: () -> Unit,
    ): FilePlayback?
}

/**
 * A music file played by a [MusicFilePlayer]. Unlike a [Playback], its resume may fail: the file may no longer open.
 */
public interface FilePlayback {
    /** Whether this file still plays, as [Playback.isPlaying]. */
    public val isPlaying: Flow<Boolean>

    /** Stops this file; does nothing if it was replaced by another one, or stopped already. */
    public fun stop()

    /**
     * Plays the same file again from the place this playback stopped at, in place of the file playing, if any; null
     * if the file can no longer be opened. A file that fails before it starts tells [onUnplayable], as for a play.
     */
    public fun resume(onUnplayable: () -> Unit): FilePlayback?
}
