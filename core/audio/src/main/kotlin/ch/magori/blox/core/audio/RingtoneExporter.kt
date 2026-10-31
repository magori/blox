package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import java.io.IOException

/** Writes a music of the player as a ringtone or an alarm of the device (ADR 0031). */
public interface RingtoneExporter {
    /**
     * Writes [score], the music named [name], as a new file of [kind], and returns the `content://` URI of that file,
     * as text. Each export makes a new file. Throws an [IOException] when it could not be written; nothing is then left
     * behind. Throws an [IllegalArgumentException], before anything is written, for a score whose bar lasts more than
     * 60 s, which no music of the player reaches. Cancelled while the file is written, the export may still finish it:
     * the file is then left without its URI being returned.
     */
    @Throws(IOException::class)
    public suspend fun export(
        score: Score,
        name: String,
        kind: RingtoneKind,
    ): String
}
