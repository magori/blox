package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Plays every [Music], one at a time: the BloX scores on [scores], the music files of the player on [files]. Playing
 * or resuming the one stops the other, so that a preview of a file never mixes with the score of a game, nor the
 * reverse. A music file that cannot be played plays the [fallback] score in its place, so that the game is never
 * silent for a file that is no longer there: a file that cannot be opened (its permission lost, the file deleted), at
 * a play as at a resume, and a file that opens but fails before it starts (a format the device cannot decode). The
 * playback of the file then plays the fallback, without telling it stopped, and resumes the fallback.
 *
 * The tempo goes to the scores only: a file plays at its own speed, the tempo of the danger belonging to the scores
 * written for it. Call it from the main thread, as the players it switches between.
 */
public class MusicSwitch(
    private val scores: Player<Score>,
    private val files: MusicFilePlayer,
    private val fallback: Score,
) : MusicPlayer {
    /** The playback last started through the switch: playing a music of the other kind stops it. */
    private var current: Playback? = null

    /** Whether [current] plays a file: only a music of the other kind needs to stop it. */
    private var currentIsFile = false

    override fun play(music: Music): Playback = when (music) {
        is Score -> playScore { scores.play(music) }
        is MusicFile -> playFile { onUnplayable -> files.play(music, onUnplayable) }
    }

    override fun setTempoFactor(factor: Double): Unit = scores.setTempoFactor(factor)

    override fun prepareTempo(factor: Double): Unit = scores.prepareTempo(factor)

    /** Plays the score [begin] starts, once the file playing, if any, is stopped. */
    private fun playScore(begin: () -> Playback): Playback {
        if (currentIsFile) current?.stop()
        return ScorePlay(begin()).also { becomeCurrent(it, isFile = false) }
    }

    /**
     * Plays the file [open] opens, telling it what to do if it fails before it starts; the score playing, if any, stops
     * once the file opened. A file that cannot be opened plays the [fallback].
     */
    private fun playFile(open: (onUnplayable: () -> Unit) -> FilePlayback?): Playback {
        val play = FilePlay()
        val file = open(play::fallBack) ?: return playScore { scores.play(fallback) }
        if (!currentIsFile) current?.stop()
        play.start(file)
        becomeCurrent(play, isFile = true)
        return play
    }

    private fun becomeCurrent(
        playback: Playback,
        isFile: Boolean,
    ) {
        current = playback
        currentIsFile = isFile
    }

    /** A score played through the switch: resumed, it stops the file playing, if any. */
    private inner class ScorePlay(private val playback: Playback) : Playback by playback {
        override fun resume(): Playback = playScore { playback.resume() }
    }

    /**
     * A music file played through the switch, or the [fallback] once the file failed before it started: its state
     * follows the one playing, so that the switch to the fallback never tells, for an instant, that it stopped.
     */
    private inner class FilePlay : Playback {
        /** The file, while it is the one played. */
        private var file: FilePlayback? = null

        /** The fallback, once the file could not be played. */
        private var fellBack: Playback? = null

        private val source = MutableStateFlow<Flow<Boolean>>(MutableStateFlow(true))

        @OptIn(ExperimentalCoroutinesApi::class)
        override val isPlaying: Flow<Boolean> = source.flatMapLatest { it }.distinctUntilChanged()

        fun start(playback: FilePlayback) {
            file = playback
            source.value = playback.isPlaying
        }

        /** The file failed before it started: the fallback plays in its place, unless another music played since. */
        fun fallBack() {
            if (current !== this) return
            val score = scores.play(fallback)
            fellBack = score
            file = null
            currentIsFile = false
            source.value = score.isPlaying
        }

        override fun stop() {
            file?.stop()
            fellBack?.stop()
        }

        override fun resume(): Playback {
            fellBack?.let { score -> return playScore { score.resume() } }
            val stopped = file ?: return playScore { scores.play(fallback) }
            return playFile { onUnplayable -> stopped.resume(onUnplayable) }
        }
    }
}
