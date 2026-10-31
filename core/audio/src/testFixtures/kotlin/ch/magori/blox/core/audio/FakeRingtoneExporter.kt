package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import java.io.IOException

/**
 * An exporter of ringtones that writes nothing: it notes each export asked for, in order, the failed ones included, and
 * fails with [failure] while it is set. Each export it accepts gets a URI of its own.
 */
class FakeRingtoneExporter : RingtoneExporter {
    /** An export asked for: the score, the name of the music and what it rings for. */
    data class Export(val score: Score, val name: String, val kind: RingtoneKind)

    /** The exports asked for, in order, the failed ones included. */
    val exports = mutableListOf<Export>()

    /** The failure the next exports throw; none when null. */
    var failure: IOException? = null

    override suspend fun export(
        score: Score,
        name: String,
        kind: RingtoneKind,
    ): String {
        exports += Export(score, name, kind)
        failure?.let { throw it }
        return "content://media/external_primary/audio/media/${exports.size}"
    }
}
