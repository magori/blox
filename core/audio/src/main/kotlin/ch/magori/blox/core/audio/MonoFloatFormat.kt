package ch.magori.blox.core.audio

import android.media.AudioFormat

/** The encoding of every track of BloX: 32-bit float samples, from −1 to 1, as the synthesizer renders them. */
internal const val MONO_FLOAT_ENCODING: Int = AudioFormat.ENCODING_PCM_FLOAT

/** The channels of every track of BloX: one, the chiptune is mono. */
internal const val MONO_CHANNELS: Int = AudioFormat.CHANNEL_OUT_MONO

/** The format of every track of BloX, the music and the effects alike: mono float samples at [sampleRate]. */
internal fun monoFloatFormat(sampleRate: Int): AudioFormat = AudioFormat.Builder()
    .setEncoding(MONO_FLOAT_ENCODING)
    .setSampleRate(sampleRate)
    .setChannelMask(MONO_CHANNELS)
    .build()
