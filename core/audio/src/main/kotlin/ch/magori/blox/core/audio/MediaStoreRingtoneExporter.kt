package ch.magori.blox.core.audio

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ch.magori.blox.core.music.Score
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Writes the ringtones through `MediaStore`, without any permission, from Android 10 on (ADR 0031): rendered by
 * [synthesizer], the synthesizer of the music, then written by [contentResolver], all of it on [dispatcher]. One
 * dispatcher, the one of input and output, for the rendering too: a few seconds of work at most, off the main thread,
 * which the export of a single file does not need to split.
 */
@RequiresApi(Build.VERSION_CODES.Q)
public class MediaStoreRingtoneExporter(
    synthesizer: Synthesizer,
    contentResolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher,
) : RingtoneExporter {
    private val writer: RingtoneWriter<Uri> = RingtoneWriter(synthesizer, ContentResolverRingtoneStore(contentResolver))

    override suspend fun export(
        score: Score,
        name: String,
        kind: RingtoneKind,
    ): String = withContext(dispatcher) { writer.write(score, name, kind).toString() }
}
