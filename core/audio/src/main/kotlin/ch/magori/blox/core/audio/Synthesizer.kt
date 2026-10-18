package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlin.math.roundToInt

/**
 * Pure sample generator: turns voices into samples from −1 to 1 at [sampleRate] samples per second.
 * It knows neither AudioTrack nor threads; the sequencer plays what it renders.
 */
public class Synthesizer(public val sampleRate: Int) {
    init {
        require(sampleRate > 0) { "The sample rate is a positive number of samples per second, was $sampleRate" }
    }

    /**
     * Renders [score]: its voices, mixed, at its tempo times [tempoFactor]. The only entry point of the
     * rendering: the sequencer plays what it returns.
     */
    public fun render(
        score: Score,
        tempoFactor: Double = 1.0,
    ): FloatArray {
        require(tempoFactor > 0 && tempoFactor.isFinite()) {
            "The tempo factor is positive and finite, was $tempoFactor"
        }
        return mix(score.voices.map { renderVoice(it, score.bpm * tempoFactor) })
    }

    /**
     * Renders [voice] at [bpm]. Each note starts at the sample of its cumulated beats, so that rounding
     * never adds up along the voice.
     */
    internal fun renderVoice(
        voice: Voice,
        bpm: Double,
    ): FloatArray {
        require(bpm > 0) { "The tempo is a positive number of beats per minute, was $bpm" }
        val boundaries = voice.notes.runningFold(0.0) { beats, note -> beats + note.beats }.map { sampleAt(it, bpm) }
        val samples = FloatArray(boundaries.last())
        voice.notes.forEachIndexed { index, note ->
            val start = boundaries[index]
            writeNote(samples, start, boundaries[index + 1] - start, note.pitch, voice.waveform, voice.volume)
        }
        return samples
    }

    /** The sample at which [beats] beats have passed at [bpm]. */
    private fun sampleAt(
        beats: Double,
        bpm: Double,
    ): Int = (beats * SECONDS_PER_MINUTE / bpm * sampleRate).roundToInt()

    /**
     * Writes one note of [sampleCount] samples into [target] from [offset]: volume × wave, the phase
     * at 0 on its first sample, shaped by the envelope. Nothing for a rest (a `null` [pitch]), since
     * [target] starts silent. No array is allocated for the note.
     */
    private fun writeNote(
        target: FloatArray,
        offset: Int,
        sampleCount: Int,
        pitch: Pitch?,
        waveform: Waveform,
        volume: Double,
    ) {
        if (pitch == null) return
        for (index in 0 until sampleCount) {
            val wave = volume * waveform.amplitudeAt(phaseAt(index, pitch.frequency))
            target[offset + index] = (wave * envelopeGain(index, sampleCount)).toFloat()
        }
    }

    /** The phase, from 0 to 1, of a wave of [frequency] at sample [index] of a note. */
    private fun phaseAt(
        index: Int,
        frequency: Double,
    ): Double = (index * frequency / sampleRate) % 1.0

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
