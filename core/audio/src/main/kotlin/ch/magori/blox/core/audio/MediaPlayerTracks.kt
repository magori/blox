package ch.magori.blox.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import ch.magori.blox.core.music.MusicFile
import java.io.IOException

/**
 * The [MusicTracks] of `MediaPlayer`, which reads every format the device decodes (MP3, AAC, Ogg, FLAC, WAV…), with
 * the attributes of the music, so that the focus and the volume are those of the scores. Opened on the main thread,
 * a `MediaPlayer` tells its errors there. A file that cannot be opened, and a track that fails, go to [onFailure], to
 * be logged. Kept thin on purpose: the logic is in [AudioFilePlayer], tested on fake tracks; the instrumented
 * `MediaPlayerTracksTest` plays a real file.
 */
public class MediaPlayerTracks(context: Context, private val onFailure: (Exception) -> Unit = {}) : MusicTracks {
    private val context: Context = context.applicationContext

    override fun open(
        file: MusicFile,
        startAt: Int,
        onError: (beforeStart: Boolean) -> Unit,
    ): MusicTrack? {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(MUSIC_ATTRIBUTES)
            // Opens the document at once: a permission lost or a file deleted fails here, most of the time. When the
            // resolver cannot open it, MediaPlayer tries the uri again as a path, which fails only when it prepares:
            // a failure before the start, as a format it cannot decode.
            player.setDataSource(context, Uri.parse(file.uri))
        } catch (failure: Exception) {
            player.release()
            if (failure !is IOException && failure !is SecurityException && failure !is IllegalArgumentException) {
                throw failure
            }
            onFailure(failure)
            return null
        }
        return MediaPlayerTrack(player, startAt) { failure, beforeStart ->
            onFailure(failure)
            onError(beforeStart)
        }
    }
}

/**
 * A file read by [player] in a loop, from [startAt] milliseconds once it is prepared, in the background. An error
 * goes to [onError], once, with whether it came before the start; the track then plays no more.
 */
private class MediaPlayerTrack(
    private val player: MediaPlayer,
    private val startAt: Int,
    onError: (IOException, beforeStart: Boolean) -> Unit,
) : MusicTrack {
    /** Whether the player was prepared, and so started: an error before that comes before the start. */
    private var prepared = false

    /** Whether the player failed: it then tells no position, which only a player in order has. */
    private var failed = false

    init {
        player.isLooping = true
        player.setOnPreparedListener {
            prepared = true
            if (startAt > 0) player.seekTo(startAt.toLong(), MediaPlayer.SEEK_CLOSEST)
            player.start()
        }
        player.setOnErrorListener { _, what, extra ->
            failed = true
            onError(IOException("The music file failed: what $what, extra $extra"), !prepared)
            // Handled: the player does not go on to tell a completion.
            true
        }
        player.prepareAsync()
    }

    /** Where the file plays; where it was to start from, until it is prepared, and once it failed. */
    override val position: Int get() = if (prepared && !failed) player.currentPosition else startAt

    override fun release() {
        // A released player tells nothing more: neither preparation nor error.
        player.release()
    }
}
