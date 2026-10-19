package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score

/**
 * What a game needs of the music: play a score in a loop, change its tempo. Its failures never reach
 * the caller: the music stops, the game goes on. [ChiptuneSequencer] is the player of the app; it reports
 * its failures to its `onFailure`, called on a thread of `Dispatchers.IO`, not on the main thread.
 */
public interface MusicPlayer {
    /**
     * Plays [score] in a loop, from its start and at its tempo (1.0), in place of the score playing, if
     * any: a tempo asked for before is forgotten, so that a music played anew (a preview, a new game) is
     * never left faster by the one before. The [Playback] stops this score only: once another one
     * replaced it, stopping it does nothing.
     */
    public fun play(score: Score): Playback

    /**
     * Sets the tempo factor of the music: 1.0 is the tempo of the score. Kept, when nothing plays, for
     * the next [Playback.resume], not for the next [play].
     */
    public fun setTempoFactor(factor: Double)
}

/** A score played by a [MusicPlayer]. */
public interface Playback {
    /** Stops this score; does nothing if it was replaced by another one, or stopped already. */
    public fun stop()

    /**
     * Plays the same score again from where this playback stopped, at the start of the beat it was in,
     * so that no note is cut, and at the tempo asked for now; in place of the score playing, if any.
     * A playback that never wrote anything resumes from where it started: the start, unless it was
     * resumed itself. Gives the new playback.
     *
     * The place kept is that of the last slice written, ahead of what was heard: the buffer of the
     * sink is dropped on stop (about 100 to 350 ms with `AudioTrack`). Going back to the start of the
     * beat makes up for it most of the time; at worst, the music resumes past a bit never heard.
     * The place is read on the main thread: called right after [stop], before the loop has seen it,
     * it may be a slice late (about 23 ms), which a game does not hear.
     */
    public fun resume(): Playback
}
