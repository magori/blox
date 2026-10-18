package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score

/**
 * What a game needs of the music: play a score in a loop, change its tempo. Its failures never reach
 * the caller: the music stops, the game goes on. [ChiptuneSequencer] is the player of the app; it reports
 * its failures to its `onFailure`, called on a thread of `Dispatchers.IO`, not on the main thread.
 */
public interface MusicPlayer {
    /**
     * Plays [score] in a loop, from its start, in place of the score playing, if any. The [Playback]
     * stops this score only: once another one replaced it, stopping it does nothing.
     */
    public fun play(score: Score): Playback

    /** Sets the tempo factor of the music: 1.0 is the tempo of the score. Kept for the next play. */
    public fun setTempoFactor(factor: Double)
}

/** A score played by a [MusicPlayer]. */
public fun interface Playback {
    /** Stops this score; does nothing if it was replaced by another one, or stopped already. */
    public fun stop()
}
