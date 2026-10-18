package ch.magori.blox.feature.game

import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.music.Score

/**
 * A music player that notes what it is told, in order (`play <title>`, `stop`, `tempo <factor>`), and
 * keeps the title [playing]. As the real one, it plays one score at a time: stopping a playback that
 * was replaced leaves the new one playing.
 */
internal class FakeMusicPlayer : MusicPlayer {
    val calls = mutableListOf<String>()
    var playing: String? = null
        private set
    private var current: Playback? = null

    override fun play(score: Score): Playback {
        calls += "play ${score.title}"
        playing = score.title
        return object : Playback {
            override fun stop() {
                calls += "stop"
                if (current === this) playing = null
            }
        }.also { current = it }
    }

    override fun setTempoFactor(factor: Double) {
        calls += "tempo $factor"
    }
}
