package ch.magori.blox.core.audio

import android.media.AudioAttributes
import android.media.AudioTrack

/**
 * An [EffectTrack] on an [AudioTrack] in static mode: the [samples] of one effect, mono, 32-bit float at
 * [sampleRate], loaded once into the track, so that each [play] only rewinds and starts it. Low latency is asked for
 * (`PERFORMANCE_MODE_LOW_LATENCY`), at the rate of the output ([effectSampleRate]) so that no resampling stands in
 * the way; whether the system grants the fast path is its choice (the device must have one, and some refuse it to
 * float samples), never sure. Kept thin on purpose, as [AudioTrackSink]: what to play and when, and its failures, are
 * tested on the JVM with [SoundEffectTracks].
 *
 * A sound effect of a game ([EFFECT_ATTRIBUTES]), mixed by the system over the music, which goes on: it asks for no
 * audio focus, being a few hundred milliseconds at most.
 *
 * @throws RuntimeException (`UnsupportedOperationException`, `IllegalStateException`…) if the device cannot open the
 * track or load the samples; the track is then released.
 */
public class StaticAudioTrack(samples: FloatArray, sampleRate: Int) : EffectTrack {
    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(EFFECT_ATTRIBUTES)
        .setAudioFormat(monoFloatFormat(sampleRate))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        .setBufferSizeInBytes(samples.size * Float.SIZE_BYTES)
        .build()

    init {
        try {
            val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            check(written == samples.size) { "The effect track took $written of ${samples.size} samples" }
            check(track.state == AudioTrack.STATE_INITIALIZED) { "The effect track did not initialize" }
        } catch (failure: RuntimeException) {
            track.release()
            throw failure
        }
    }

    /** Stops the effect where it stands, rewinds it to its start, and plays it. */
    override fun play() {
        track.stop()
        val rewound = track.reloadStaticData()
        check(rewound == AudioTrack.SUCCESS) { "The effect track could not rewind, error $rewound" }
        track.play()
    }

    override fun release() {
        track.release()
    }

    private companion object {
        val EFFECT_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
