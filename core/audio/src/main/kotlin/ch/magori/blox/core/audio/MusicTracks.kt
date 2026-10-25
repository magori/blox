package ch.magori.blox.core.audio

import ch.magori.blox.core.music.MusicFile

/**
 * Opens a music file as a track playing in a loop. [MediaPlayerTracks] opens them on `MediaPlayer`; [AudioFilePlayer]
 * keeps the logic, tested on fake tracks.
 */
public interface MusicTracks {
    /**
     * Opens [file] and plays it in a loop from [startAt] milliseconds; null if it cannot be opened (its permission
     * lost, the file deleted). [onError] hears, on the main thread, that the track failed, and whether it failed
     * before it started playing (a format it cannot decode) or while it played: the track then plays no more, and is
     * to be released.
     */
    public fun open(
        file: MusicFile,
        startAt: Int,
        onError: (beforeStart: Boolean) -> Unit,
    ): MusicTrack?
}

/** A music file playing in a loop, until it is released. */
public interface MusicTrack {
    /** Where the track stands in the file, in milliseconds. */
    public val position: Int

    /** Plays at [gain], from 0 (silent) to 1 (the file as it is, the gain of a new track). */
    public fun setGain(gain: Float)

    /** Stops the track for good, and frees what it holds. */
    public fun release()
}
