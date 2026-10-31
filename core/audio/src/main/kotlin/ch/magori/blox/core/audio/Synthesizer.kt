package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.Tone
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlin.math.roundToInt

/**
 * Pure sample generator: turns voices and sound effects into samples from −1 to 1 at [sampleRate] samples per second.
 * It knows neither AudioTrack nor threads; the sequencer plays the scores it renders, [SoundEffectTracks] the effects.
 */
public class Synthesizer(public val sampleRate: Int) {
    init {
        require(sampleRate > 0) { "The sample rate is a positive number of samples per second, was $sampleRate" }
    }

    /**
     * Renders [score]: its voices, mixed, at its tempo times [tempoFactor]. The only entry point of the rendering of
     * the scores: the sequencer plays what it returns.
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
     * Renders [effect]: its blips one after the other, each starting at the sample of its cumulated milliseconds,
     * so that rounding never adds up along the effect, and each shaped by the envelope of the effect.
     */
    public fun render(effect: SoundEffect): FloatArray {
        val boundaries = effect.blips.runningFold(0) { millis, blip -> millis + blip.millis }.map(::sampleAtMillis)
        val samples = FloatArray(boundaries.last())
        val attack = sampleAtMillis(effect.envelope.attackMillis)
        val release = sampleAtMillis(effect.envelope.releaseMillis)
        effect.blips.forEachIndexed { index, blip ->
            val start = boundaries[index]
            val gain = { sample: Int, count: Int -> blipGain(sample, count, attack, release) }
            val tone = arcadeTone(effect.waveform)
            writeNote(samples, start, boundaries[index + 1] - start, blip.pitch, tone, effect.volume, gain)
        }
        return samples
    }

    /** The sample at which [millis] milliseconds have passed. */
    private fun sampleAtMillis(millis: Int): Int = (millis.toDouble() * sampleRate / MILLIS_PER_SECOND).roundToInt()

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
            writeNote(
                samples,
                start,
                boundaries[index + 1] - start,
                note.pitch,
                arcadeTone(voice.waveform),
                voice.volume,
                ::noteGain,
            )
        }
        return samples
    }

    /** The sample at which [beats] beats have passed at [bpm]. */
    private fun sampleAt(
        beats: Double,
        bpm: Double,
    ): Int = (beats * SECONDS_PER_MINUTE / bpm * sampleRate).roundToInt()

    /**
     * Writes one note of [sampleCount] samples into [target] from [offset]: volume × wave of [tone], the phase
     * at 0 on its first sample, shaped by [gain] of the sample and of [sampleCount]. Nothing for a rest
     * (a `null` [pitch]), since [target] starts silent. No array is allocated for the note.
     */
    private inline fun writeNote(
        target: FloatArray,
        offset: Int,
        sampleCount: Int,
        pitch: Pitch?,
        tone: Tone,
        volume: Double,
        gain: (index: Int, sampleCount: Int) -> Double,
    ) {
        if (pitch == null) return
        val phaseStep = pitch.frequency / sampleRate
        for (index in 0 until sampleCount) {
            val wave = volume * tone.amplitudeAt(phaseAt(index, pitch.frequency), phaseStep)
            target[offset + index] = (wave * gain(index, sampleCount)).toFloat()
        }
    }

    /**
     * Gain from 0 to 1 at sample [index] of a blip of [sampleCount] samples: a linear rise from silence over
     * [attack] samples, full, then a linear fall over its last [release] samples, down towards silence.
     */
    private fun blipGain(
        index: Int,
        sampleCount: Int,
        attack: Int,
        release: Int,
    ): Double {
        val rise = if (index < attack) index.toDouble() / attack else 1.0
        val fall = if (sampleCount - index < release) (sampleCount - index).toDouble() / release else 1.0
        return minOf(rise, fall)
    }

    /** The tone of [waveform] in the Arcade style, the sound of BloX before the instruments. */
    private fun arcadeTone(waveform: Waveform): Tone = SoundStyle.ARCADE.instrumentFor(waveform).tone

    /** The phase, from 0 to 1, of a wave of [frequency] at sample [index] of a note. */
    private fun phaseAt(
        index: Int,
        frequency: Double,
    ): Double = (index * frequency / sampleRate) % 1.0

    /**
     * Gain from 0 to 1 at sample [index] of a note of [sampleCount] samples: full up to 80 % of the
     * note, a linear fade out up to 95 %, then silence, so that repeated notes stay distinct.
     */
    private fun noteGain(
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
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
