package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import java.io.IOException

/**
 * Writes a score as a ringtone into [store] (ADR 0031): rendered by [synthesizer], the synthesizer of the music, at the
 * tempo of the score, made a ringtone ([ringtoneSamples]) and encoded as WAV ([WavEncoder]), before any entry is made.
 * The entry is published once its file is whole; a write that fails removes it and throws on.
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
        val entry = store.insert(ringtoneFileName(name), kind)
        try {
            store.output(entry).use { it.write(wav) }
            store.publish(entry)
        } catch (failure: IOException) {
            store.delete(entry)
            throw failure
        }
        return entry
    }
}

private const val SECONDS_PER_MINUTE = 60.0
