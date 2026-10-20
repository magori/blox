package ch.magori.blox.core.audio

import android.content.Context
import android.media.AudioManager

/**
 * The sample rate to render the sound effects at: the one of the output of the device
 * (`AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE`), since a track at another rate is resampled, which keeps it off the
 * fast path of the output. Its own rate, apart from the one of the music, which latency does not matter to.
 */
public fun effectSampleRate(context: Context): Int = effectSampleRateOf(
    context.getSystemService(AudioManager::class.java)?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE),
)

/** The sample rate of the effects for the [outputSampleRate] the device tells, 48 000 Hz if it tells none that holds. */
internal fun effectSampleRateOf(outputSampleRate: String?): Int =
    outputSampleRate?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_EFFECT_SAMPLE_RATE

/** The sample rate of the output of most devices, when the device does not tell its own. */
private const val DEFAULT_EFFECT_SAMPLE_RATE = 48_000
