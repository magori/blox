package ch.magori.blox.core.audio

import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A player of music files that notes in [log] what it is told (`play file <uri>`, `stop file`, `resume file <uri>`),
 * keeps the file [playing] and the [volume] set, and cannot open the files [unreadable] (a permission lost, a file
 * deleted). The file playing may turn out undecodable before it starts ([failBeforeStart]). As the real one, it plays
 * one file at a time: a new play or resume ends the playback before.
 */
internal class FakeMusicFilePlayer(
    private val log: MutableList<String> = mutableListOf(),
    val unreadable: MutableSet<MusicFile> = mutableSetOf(),
) : MusicFilePlayer {
    var playing: MusicFile? = null
        private set
    var volume: Volume = Volume.FULL
        private set
    private var current: FakeFilePlayback? = null

    override fun setVolume(volume: Volume) {
        this.volume = volume
    }

    override fun play(
        file: MusicFile,
        onUnplayable: () -> Unit,
    ): FilePlayback? = open(file, onUnplayable, verb = "play")

    /** The file playing fails before it started, as a format the device cannot decode: it tells so, then ends. */
    fun failBeforeStart() {
        val failing = current ?: return
        failing.onUnplayable()
        failing.end()
    }

    private fun open(
        file: MusicFile,
        onUnplayable: () -> Unit,
        verb: String,
    ): FilePlayback? {
        if (file in unreadable) return null
        log += "$verb file ${file.uri}"
        current?.end()
        playing = file
        return FakeFilePlayback(file, onUnplayable).also { current = it }
    }

    private inner class FakeFilePlayback(private val file: MusicFile, val onUnplayable: () -> Unit) : FilePlayback {
        private val state = MutableStateFlow(true)

        override val isPlaying: Flow<Boolean> = state

        fun end() {
            state.value = false
            if (current === this) {
                current = null
                playing = null
            }
        }

        override fun stop() {
            log += "stop file"
            end()
        }

        override fun resume(onUnplayable: () -> Unit): FilePlayback? = open(file, onUnplayable, verb = "resume")
    }
}
