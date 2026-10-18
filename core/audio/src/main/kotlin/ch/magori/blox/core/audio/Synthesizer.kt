package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlin.math.roundToInt

/**
 * Pure sample generator: turns voices into samples from −1 to 1 at [sampleRate] samples per second.
 * It knows neither AudioTrack nor threads; the sequencer plays what it renders.
 */
public class Synthesizer(private val sampleRate: Int) {
    init {
        require(sampleRate > 0) { "The sample rate is a positive number of samples per second, was $sampleRate" }
    }

    /**
     * Renders [voice] at [bpm]. Each note starts at the sample of its cumulated beats, so that rounding
     * never adds up along the voice.
     */
    public fun renderVoice(
        voice: Voice,
        bpm: Int,
    ): FloatArray {
        require(bpm > 0) { "The tempo is a positive number of beats per minute, was $bpm" }
        val boundaries = voice.notes.runningFold(0.0) { beats, note -> beats + note.beats }.map { sampleAt(it, bpm) }
        val samples = FloatArray(boundaries.last())
        voice.notes.forEachIndexed { index, note ->
            val start = boundaries[index]
            renderNote(note.pitch, voice.waveform, voice.volume, boundaries[index + 1] - start)
                .copyInto(samples, destinationOffset = start)
        }
        return samples
    }

    /** The sample at which [beats] beats have passed at [bpm]. */
    private fun sampleAt(
        beats: Double,
        bpm: Int,
    ): Int = (beats * SECONDS_PER_MINUTE / bpm * sampleRate).roundToInt()

    /**
     * One note of [sampleCount] samples, shaped by the envelope; a `null` [pitch] is a rest, which is
     * silent.
     */
    internal fun renderNote(
        pitch: Pitch?,
        waveform: Waveform,
        volume: Double,
        sampleCount: Int,
    ): FloatArray {
        if (pitch == null) return FloatArray(sampleCount)
        val samples = oscillate(waveform, pitch.frequency, volume, sampleCount)
        for (index in samples.indices) samples[index] *= envelopeGain(index, sampleCount).toFloat()
        return samples
    }

    /** [sampleCount] samples of volume × wave, without envelope; the phase is 0 at the first sample. */
    internal fun oscillate(
        waveform: Waveform,
        frequency: Double,
        volume: Double,
        sampleCount: Int,
    ): FloatArray = FloatArray(sampleCount) { index ->
        val phase = (index * frequency / sampleRate) % 1.0
        (volume * waveform.amplitudeAt(phase)).toFloat()
    }

    /**
     * Gain from 0 to 1 at sample [index] of a note of [sampleCount] samples: full up to 80 % of the
     * note, a linear fade out up to 95 %, then silence, so that repeated notes stay distinct.
     */
    private fun envelopeGain(
        index: Int,
        sampleCount: Int,
    ): Double {
        val position = index.toDouble() / sampleCount
        return when {
            position < SUSTAIN_END -> 1.0
            position < FADE_END -> (FADE_END - position) / (FADE_END - SUSTAIN_END)
            else -> 0.0
        }
    }

    private companion object {
        const val SUSTAIN_END = 0.80
        const val FADE_END = 0.95
        const val SECONDS_PER_MINUTE = 60.0
    }
}
