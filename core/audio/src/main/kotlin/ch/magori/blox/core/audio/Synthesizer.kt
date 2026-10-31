package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Adsr
import ch.magori.blox.core.music.Echo
import ch.magori.blox.core.music.Gate
import ch.magori.blox.core.music.Instrument
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.Tone
import ch.magori.blox.core.music.Voice
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
     * Renders [score]: its voices, mixed, at its tempo times [tempoFactor], each on the instrument [style] gives to
     * its waveform, then the echo of [style], if it has one, on the mix, wrapped around the start of the loop. The
     * only entry point of the rendering of the scores: the sequencer plays what it returns.
     */
    public fun render(
        score: Score,
        tempoFactor: Double = 1.0,
        style: SoundStyle = SoundStyle.ARCADE,
    ): FloatArray {
        require(tempoFactor > 0 && tempoFactor.isFinite()) {
            "The tempo factor is positive and finite, was $tempoFactor"
        }
        val bpm = score.bpm * tempoFactor
        val mixed = mix(score.voices.map { renderVoice(it, bpm, style.instrumentFor(it.waveform)) })
        return style.echo?.let { withEcho(mixed, it, bpm) } ?: mixed
    }

    /**
     * Renders [effect]: its blips one after the other, each starting at the sample of its cumulated milliseconds,
     * so that rounding never adds up along the effect, and each shaped by the envelope of the effect.
     */
    public fun render(
        effect: SoundEffect,
        style: SoundStyle = SoundStyle.ARCADE,
    ): FloatArray = render(effect, style.instrumentFor(effect.waveform))

    /**
     * Renders [effect] on the tone of [instrument], then through its low-pass if it has one; the blips keep the
     * envelope of the effect, not the shape of the notes of [instrument].
     */
    internal fun render(
        effect: SoundEffect,
        instrument: Instrument,
    ): FloatArray {
        val boundaries = effect.blips.runningFold(0) { millis, blip -> millis + blip.millis }.map(::sampleAtMillis)
        val samples = FloatArray(boundaries.last())
        val attack = sampleAtMillis(effect.envelope.attackMillis)
        val release = sampleAtMillis(effect.envelope.releaseMillis)
        effect.blips.forEachIndexed { index, blip ->
            val start = boundaries[index]
            val gain = { sample: Int, count: Int -> blipGain(sample, count, attack, release) }
            writeNote(samples, start, boundaries[index + 1] - start, blip.pitch, instrument.tone, effect.volume, gain)
        }
        return filtered(samples, instrument)
    }

    /** [samples] through the low-pass of [instrument], or themselves if it has none. */
    private fun filtered(
        samples: FloatArray,
        instrument: Instrument,
    ): FloatArray = instrument.lowPass?.let { lowPass(samples, it, sampleRate) } ?: samples

    /** The sample at which [millis] milliseconds have passed. */
    private fun sampleAtMillis(millis: Int): Int = (millis.toDouble() * sampleRate / MILLIS_PER_SECOND).roundToInt()

    /**
     * Renders [voice] at [bpm] on [instrument], by default the one of the Arcade style. Each note starts at the
     * sample of its cumulated beats, so that rounding never adds up along the voice. The whole voice then passes the
     * low-pass of [instrument], if it has one.
     */
    internal fun renderVoice(
        voice: Voice,
        bpm: Double,
        instrument: Instrument = SoundStyle.ARCADE.instrumentFor(voice.waveform),
    ): FloatArray {
        require(bpm > 0) { "The tempo is a positive number of beats per minute, was $bpm" }
        val boundaries = voice.notes.runningFold(0.0) { beats, note -> beats + note.beats }.map { sampleAt(it, bpm) }
        val samples = FloatArray(boundaries.last())
        val shape = instrument.shape
        val adsr = if (shape is Adsr) AdsrGain(shape) else null
        voice.notes.forEachIndexed { index, note ->
            val start = boundaries[index]
            val sampleCount = boundaries[index + 1] - start
            // One call of the inline writeNote per shape, so that the gain is inlined too, without boxing a sample
            if (adsr == null) {
                writeNote(samples, start, sampleCount, note.pitch, instrument.tone, voice.volume, ::gateGain)
            } else {
                writeNote(samples, start, sampleCount, note.pitch, instrument.tone, voice.volume, adsr::at)
            }
        }
        return filtered(samples, instrument)
    }

    /**
     * The gain from 0 to 1 at a sample of a note shaped by [adsr], its durations counted once in samples: a linear
     * rise from silence over its attack, a linear fall to its sustain over its decay, the sustain held, then, over the
     * last samples of the note, a linear release from the level reached down towards silence. The release ends on the
     * last sample of the note, so that it never runs into the next one: in a note shorter than it, it is shortened,
     * and starts once the attack is over.
     */
    private inner class AdsrGain(private val adsr: Adsr) {
        private val attack = sampleAtMillis(adsr.attackMillis)
        private val decay = sampleAtMillis(adsr.decayMillis)
        private val release = sampleAtMillis(adsr.releaseMillis)

        /** The gain at sample [index] of a note of [sampleCount] samples. */
        fun at(
            index: Int,
            sampleCount: Int,
        ): Double {
            val releaseStart = maxOf(sampleCount - release, minOf(attack, sampleCount))
            return if (index < releaseStart) {
                level(index)
            } else {
                level(releaseStart) * (sampleCount - index) / (sampleCount - releaseStart)
            }
        }

        private fun level(index: Int): Double = when {
            index < attack -> index.toDouble() / attack
            index < attack + decay -> 1 - (1 - adsr.sustain) * (index - attack) / decay
            else -> adsr.sustain
        }
    }

    /** [samples] of a music at [bpm] with [echo], its delay counted in beats so that it follows the tempo. */
    internal fun withEcho(
        samples: FloatArray,
        echo: Echo,
        bpm: Double,
    ): FloatArray = withEcho(samples, echo, sampleAt(echo.delayBeats, bpm))

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

    /** The phase, from 0 to 1, of a wave of [frequency] at sample [index] of a note. */
    private fun phaseAt(
        index: Int,
        frequency: Double,
    ): Double = (index * frequency / sampleRate) % 1.0

    /**
     * Gain from 0 to 1 at sample [index] of a note of [sampleCount] samples shaped by the [Gate]: full up to
     * [Gate.heldUntil] of the note, a linear fade out up to [Gate.silentFrom], then silence, so that repeated notes
     * stay distinct.
     */
    private fun gateGain(
        index: Int,
        sampleCount: Int,
    ): Double {
        val position = index.toDouble() / sampleCount
        return when {
            position < Gate.heldUntil -> 1.0
            position < Gate.silentFrom -> (Gate.silentFrom - position) / (Gate.silentFrom - Gate.heldUntil)
            else -> 0.0
        }
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
