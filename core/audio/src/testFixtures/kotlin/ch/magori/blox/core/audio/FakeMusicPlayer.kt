package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId

/**
 * A music player that notes what it is told, in order (`play <id>`, `stop`, `resume <id>`,
 * `tempo <factor>`), and keeps the identifier of the score [playing]. As the real one, it plays one
 * score at a time: stopping a playback that was replaced leaves the new one playing.
 */
class FakeMusicPlayer : MusicPlayer {
    val calls = mutableListOf<String>()
    var playing: ScoreId? = null
        private set
    private var current: Playback? = null

    override fun play(score: Score): Playback {
        calls += "play ${score.id}"
        return start(score)
    }

    override fun setTempoFactor(factor: Double) {
        calls += "tempo $factor"
    }

    private fun start(score: Score): Playback {
        playing = score.id
        return object : Playback {
            override fun stop() {
                calls += "stop"
                if (current === this) playing = null
            }

            override fun resume(): Playback {
                calls += "resume ${score.id}"
                return start(score)
            }
        }.also { current = it }
    }
}
