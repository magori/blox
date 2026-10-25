package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A music player that notes what it is told, in order (`play <id>`, `play file <uri>`, `stop`, `resume <id>`,
 * `resume file <uri>`, `tempo <factor>`), the scores it was told to play ([playedScores]), and keeps the identifier of the score [playing] or the music file
 * [playingFile], the [tempo] it plays at, the tempos [prepared] and the [volume] set, which [calls] does not note. As
 * the real one, it plays one music at a time: stopping a playback that was replaced leaves the new one playing; a new
 * play starts at the normal tempo and forgets the tempos prepared, a resume goes on at the tempo asked for, with the
 * tempos prepared; the volume stays as it was set; a tempo factor that is not positive and finite is refused. The
 * music may stop by itself ([fail]), as when the real one fails. It keeps the contract of the real one
 * (`MusicPlayerContract`).
 */
class FakeMusicPlayer : MusicPlayer {
    val calls = mutableListOf<String>()

    /** The scores played ([play], not resumed), in order: what they hold, which [calls] does not tell. */
    val playedScores = mutableListOf<Score>()
    var playing: ScoreId? = null
        private set
    var playingFile: MusicFile? = null
        private set
    var tempo: Double = 1.0
        private set
    var prepared: Set<Double> = emptySet()
        private set
    var volume: Volume = Volume.FULL
        private set

    /** Every volume set, in order, the same one set again included. */
    val volumesSet = mutableListOf<Volume>()

    private var current: FakePlayback? = null

    override fun play(music: Music): Playback {
        calls += "play ${music.noted}"
        if (music is Score) playedScores += music
        tempo = 1.0
        prepared = emptySet()
        return start(music)
    }

    override fun setTempoFactor(factor: Double) {
        requireValidTempo(factor)
        calls += "tempo $factor"
        tempo = factor
    }

    override fun prepareTempo(factor: Double) {
        requireValidTempo(factor)
        prepared = prepared + factor
    }

    override fun setVolume(volume: Volume) {
        this.volume = volume
        volumesSet += volume
    }

    /**
     * The music playing stops by itself, as when the sequencer fails (no audio output, a write refused):
     * its playback no longer plays, and may be resumed. Noted as `failure`.
     */
    fun fail() {
        calls += "failure"
        silence()
        current?.end()
    }

    private fun start(music: Music): Playback {
        current?.end()
        silence()
        when (music) {
            is Score -> playing = music.id
            is MusicFile -> playingFile = music
        }
        return FakePlayback(music).also { current = it }
    }

    private fun silence() {
        playing = null
        playingFile = null
    }

    private inner class FakePlayback(private val music: Music) : Playback {
        private val state = MutableStateFlow(true)

        override val isPlaying: Flow<Boolean> = state

        /** This playback no longer plays: stopped, replaced, or its music stopped by itself. */
        fun end() {
            state.value = false
        }

        override fun stop() {
            calls += "stop"
            end()
            if (current === this) silence()
        }

        override fun resume(): Playback {
            calls += "resume ${music.noted}"
            return start(music)
        }
    }

    private fun requireValidTempo(factor: Double) {
        require(factor > 0 && factor.isFinite()) { "The tempo factor is positive and finite, was $factor" }
    }

    /** How a music is noted in [calls]: a score by its identifier, a file by its uri. */
    private val Music.noted: String
        get() = when (this) {
            is Score -> id.toString()
            is MusicFile -> "file $uri"
        }
}
