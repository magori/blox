package ch.magori.blox.core.audio

import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays the music files of the player on the [tracks] it opens, one at a time, in a loop, at their own speed. A
 * playback stopped or replaced keeps the place it stood at, and resumes from there; every track plays at the volume
 * set. A track that fails while it plays ends its playback ([FilePlayback.isPlaying]), as a sequencer that fails: the
 * game goes on in silence, and its resume tries again. A track that fails before it starts tells its file cannot be
 * played, then ends. Call it from the main thread, the one the tracks report their errors on.
 */
public class AudioFilePlayer(private val tracks: MusicTracks) : MusicFilePlayer {
    /** The playback whose track plays; null when none does. */
    private var current: TrackPlayback? = null

    /** The gain of the volume set, at which every track plays, from its opening. */
    private var gain = Volume.FULL.gain

    /** Plays the file playing at [volume] from now on, and the files played next. */
    override fun setVolume(volume: Volume) {
        gain = volume.gain
        current?.track?.setGain(gain)
    }

    override fun play(
        file: MusicFile,
        onUnplayable: () -> Unit,
    ): FilePlayback? = start(file, startAt = 0, onUnplayable)

    /** Opens [file] from [startAt] in place of the file playing; null, the file playing going on, if it cannot. */
    private fun start(
        file: MusicFile,
        startAt: Int,
        onUnplayable: () -> Unit,
    ): TrackPlayback? {
        val playback = TrackPlayback(file, startAt)
        val track = tracks.open(file, startAt) { beforeStart -> playback.fail(beforeStart, onUnplayable) }
            ?: return null
        track.setGain(gain)
        current?.end()
        current = playback
        return playback.apply { this.track = track }
    }

    private inner class TrackPlayback(
        private val file: MusicFile,
        /** Where the file resumes from: where it started, then where its track stood when it ended. */
        private var place: Int,
    ) : FilePlayback {
        private val state = MutableStateFlow(true)

        override val isPlaying: Flow<Boolean> = state.asStateFlow()

        /** The track of this playback, while it plays. */
        var track: MusicTrack? = null

        /** The track failed: before it started, its file cannot be played, which is told first. */
        fun fail(
            beforeStart: Boolean,
            onUnplayable: () -> Unit,
        ) {
            if (track == null) return
            if (beforeStart) onUnplayable()
            end()
        }

        /** Ends this playback, if it plays: its track is released, its place kept for the resume. */
        fun end() {
            val ended = track ?: return
            place = ended.position
            ended.release()
            track = null
            state.value = false
            if (current === this) current = null
        }

        override fun stop() = end()

        override fun resume(onUnplayable: () -> Unit): FilePlayback? = start(file, place, onUnplayable)
    }
}
