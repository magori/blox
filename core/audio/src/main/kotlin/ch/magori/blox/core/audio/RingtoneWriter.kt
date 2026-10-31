package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import java.io.IOException

/**
 * Writes a score as a ringtone into [store] (ADR 0031): rendered by [synthesizer], the synthesizer of the music, at the
 * tempo of the score, made a ringtone ([ringtoneSamples]) and encoded as WAV ([WavEncoder]), before any entry is made.
 * The entry is published once its file is whole. Any failure of the store is thrown as an [IOException]; once the
 * entry is made, it is removed first, and a removal that fails too is added to that failure as suppressed.
 */
internal class RingtoneWriter<Entry>(private val synthesizer: Synthesizer, private val store: RingtoneStore<Entry>) {
    /** Writes [score], the music named [name], as a new file of [kind], and returns its entry. */
    fun write(
        score: Score,
        name: String,
        kind: RingtoneKind,
    ): Entry {
        val sampleRate = synthesizer.sampleRate
        val samplesPerBar = score.beatsPerBar * SECONDS_PER_MINUTE / score.bpm * sampleRate
        val wav = WavEncoder.encode(ringtoneSamples(synthesizer.render(score), sampleRate, samplesPerBar), sampleRate)
        val entry = try {
            store.insert(ringtoneFileName(name), kind)
        } catch (failure: Exception) {
            throw failure.asIOException()
        }
        try {
            store.output(entry).use { it.write(wav) }
            store.publish(entry)
        } catch (failure: Exception) {
            val ioFailure = failure.asIOException()
            runCatching { store.delete(entry) }.onFailure { ioFailure.addSuppressed(it) }
            throw ioFailure
        }
        return entry
    }
}

/** This failure of the store as the [IOException] the exporter promises. */
private fun Exception.asIOException(): IOException =
    this as? IOException ?: IOException("The ringtone could not be written", this)

private const val SECONDS_PER_MINUTE = 60.0
