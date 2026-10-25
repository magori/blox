package ch.magori.blox.core.audio

import android.media.AudioTrack

/**
 * An [AudioSink] on an [AudioTrack] in stream mode: mono, 32-bit float samples at [sampleRate].
 * [write] blocks while the track buffer is full, which paces the sequencer. Kept thin on purpose: it
 * has no JVM test, the sequencer is tested with fake sinks.
 */
public class AudioTrackSink(sampleRate: Int) : AudioSink {
    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(MUSIC_ATTRIBUTES)
        .setAudioFormat(monoFloatFormat(sampleRate))
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(bufferSizeInBytes(sampleRate))
        .build()

    init {
        try {
            track.play()
        } catch (failure: IllegalStateException) {
            track.release()
            throw failure
        }
    }

    override suspend fun write(samples: FloatArray) {
        val result = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        if (result == AudioTrack.ERROR_DEAD_OBJECT) throw DeadSinkException("The AudioTrack died, error $result")
        check(result >= 0) { "The AudioTrack refused the samples, error $result" }
    }

    override fun setGain(gain: Float) {
        track.setVolume(gain)
    }

    /** Released even when dead: a track whose pause fails still frees its resources. */
    override fun release() {
        try {
            track.pause()
            track.flush()
        } finally {
            track.release()
        }
    }

    private companion object {
        /**
         * Four times the minimum the device asks for, and at least two slices of the sequencer, so that
         * a slice is always written while the previous one plays.
         */
        fun bufferSizeInBytes(sampleRate: Int): Int {
            val minimum = AudioTrack.getMinBufferSize(sampleRate, MONO_CHANNELS, MONO_FLOAT_ENCODING)
            require(minimum > 0) { "This device cannot play mono float samples at $sampleRate Hz, error $minimum" }
            return maxOf(4 * minimum, 2 * SLICE_SIZE * Float.SIZE_BYTES)
        }
    }
}
