package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The samples of a ringtone made of the [rendering] of a music, which loops, at [sampleRate] samples per second,
 * whose bars last [samplesPerBar] samples (ADR 0031). Whole loops, the fewest that last at least 20 s, so that the
 * system loops the file without a seam; a loop longer than 60 s is cut at its last whole bar before 60 s, which
 * ends on the nearest sample, as each note starts in [Synthesizer]. Its loudest sample is brought to −1 dBFS; a
 * silent rendering stays silent. An empty rendering, and a bar of no length, of less or of more than 60 s, are
 * refused.
 */
internal fun ringtoneSamples(
    rendering: FloatArray,
    sampleRate: Int,
    samplesPerBar: Double,
): FloatArray {
    require(samplesPerBar > 0.0) { "A bar lasts more than no sample, not $samplesPerBar" }
    require(samplesPerBar <= MAXIMUM_SECONDS * sampleRate) { "A bar lasts at most $MAXIMUM_SECONDS s" }
    require(rendering.isNotEmpty()) { "A ringtone is made of a rendering of at least one sample" }
    val samples = loopsOf(rendering, sampleRate, samplesPerBar)
    val loudest = samples.maxOf { abs(it) }
    if (loudest == 0f) return samples
    val gain = PEAK / loudest
    return FloatArray(samples.size) { samples[it] * gain }
}

/**
 * The length, in whole seconds, of the ringtone made of [score] (ADR 0031), as [ringtoneSamples] makes it of its
 * rendering by the synthesizer of the music: whole loops, at least 20 s; at most 60 s, cut at a whole bar.
 */
public fun ringtoneSeconds(score: Score): Int {
    val secondsPerBeat = SECONDS_PER_MINUTE / score.bpm
    val loopSamples = (score.voices.first().beats * secondsPerBeat * MUSIC_SAMPLE_RATE).roundToInt()
    val samplesPerBar = score.beatsPerBar * secondsPerBeat * MUSIC_SAMPLE_RATE
    val samples = ringtoneLength(loopSamples, MUSIC_SAMPLE_RATE, samplesPerBar)
    return (samples.toDouble() / MUSIC_SAMPLE_RATE).roundToInt()
}

private fun loopsOf(
    rendering: FloatArray,
    sampleRate: Int,
    samplesPerBar: Double,
): FloatArray {
    val length = ringtoneLength(rendering.size, sampleRate, samplesPerBar)
    return FloatArray(length) { rendering[it % rendering.size] }
}

/**
 * The number of samples of a ringtone made of a loop of [loopSamples] samples: the fewest whole loops that last at
 * least 20 s or, for a loop longer than 60 s, its last whole bar before 60 s, ending on the nearest sample.
 */
private fun ringtoneLength(
    loopSamples: Int,
    sampleRate: Int,
    samplesPerBar: Double,
): Int {
    val maximum = MAXIMUM_SECONDS * sampleRate
    if (loopSamples > maximum) return (floor(maximum / samplesPerBar) * samplesPerBar).roundToInt()
    val minimum = MINIMUM_SECONDS * sampleRate
    val loops = (minimum + loopSamples - 1) / loopSamples
    return loops * loopSamples
}

/** A ringtone lasts at least 20 s, so that its loop does not come back too often. */
private const val MINIMUM_SECONDS = 20

/** A ringtone lasts at most 60 s, 5.3 MB at 44 100 Hz. */
private const val MAXIMUM_SECONDS = 60

/** −1 dBFS, 10^(−1/20): the loudest sample of a ringtone, which the musics, near −13 dBFS, are far from. */
private const val PEAK = 0.8912509f

private const val SECONDS_PER_MINUTE = 60.0
