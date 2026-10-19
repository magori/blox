package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A music player that notes what it is told, in order (`play <id>`, `stop`, `resume <id>`,
 * `tempo <factor>`), and keeps the identifier of the score [playing], the [tempo] it plays at and the
 * tempos [prepared]. As the real one, it plays one score at a time: stopping a playback that was replaced
 * leaves the new one playing; a new play starts at the normal tempo and forgets the tempos prepared, a
 * resume goes on at the tempo asked for, with the tempos prepared; a tempo factor that is not positive and
 * finite is refused. The music may stop by itself ([fail]), as when the real one fails. It keeps the
 * contract of the real one (`MusicPlayerContract`).
 */
class FakeMusicPlayer : MusicPlayer {
    val calls = mutableListOf<String>()
    var playing: ScoreId? = null
        private set
    var tempo: Double = 1.0
        private set
    var prepared: Set<Double> = emptySet()
        private set
    private var current: FakePlayback? = null

    override fun play(score: Score): Playback {
        calls += "play ${score.id}"
        tempo = 1.0
        prepared = emptySet()
        return start(score)
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

    /**
     * The music playing stops by itself, as when the sequencer fails (no audio output, a write refused):
     * its playback no longer plays, and may be resumed. Noted as `failure`.
     */
    fun fail() {
        calls += "failure"
        playing = null
        current?.end()
    }

    private fun start(score: Score): Playback {
        current?.end()
        playing = score.id
        return FakePlayback(score).also { current = it }
    }

    private inner class FakePlayback(private val score: Score) : Playback {
        private val state = MutableStateFlow(true)

        override val isPlaying: Flow<Boolean> = state

        /** This playback no longer plays: stopped, replaced, or its music stopped by itself. */
        fun end() {
            state.value = false
        }

        override fun stop() {
            calls += "stop"
            end()
            if (current === this) playing = null
        }

        override fun resume(): Playback {
            calls += "resume ${score.id}"
            return start(score)
        }
    }

    private fun requireValidTempo(factor: Double) {
        require(factor > 0 && factor.isFinite()) { "The tempo factor is positive and finite, was $factor" }
    }
}
