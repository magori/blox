package ch.magori.blox.music

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import ch.magori.blox.core.data.MusicFileNames
import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The names of the music files of the player, asked of their provider through [resolver]
 * ([OpenableColumns.DISPLAY_NAME], what the picker of the system shows), on [io]: a provider answers through another
 * process, or reads a disk. A document gone, out of reach (its permission lost), of no provider, or that a provider
 * fails to describe has no name: it is only shown, never worth a failure. The file is chosen once its name is read
 * (the home shows them together), so a provider that does not answer within [timeout] gives no name: the choice is
 * never held up longer, the file being kept without its name.
 */
internal class DocumentMusicFileNames(
    private val resolver: ContentResolver,
    private val io: CoroutineContext = Dispatchers.IO,
    private val timeout: Duration = NAME_TIMEOUT,
) : MusicFileNames {
    // `Uri.parse` as core:audio opens the same uri (MediaPlayerTracks), rather than core-ktx, which app does not declare.
    @Suppress("UseKtx")
    override suspend fun nameOf(file: MusicFile): String? = readWithinOrNull(timeout, io) {
        try {
            resolver.query(Uri.parse(file.uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use(::displayNameIn)
        } catch (_: RuntimeException) {
            // A permission refused (SecurityException), a uri the provider does not know (IllegalArgumentException), or
            // any failure of a provider of another application: the file keeps no name, and is still chosen.
            null
        }
    }
}

/** How long the name of a file is waited for, when it is chosen: far more than a provider that answers takes. */
private val NAME_TIMEOUT: Duration = 2.seconds

/**
 * What the blocking [read] gives, run on [context], if it answers within [timeout]; null after, without waiting for it
 * any longer: a blocking call cannot be interrupted, so it is left to end by itself, its answer dropped.
 */
internal suspend fun <T : Any> readWithinOrNull(
    timeout: Duration,
    context: CoroutineContext,
    read: () -> T?,
): T? {
    val reading = CoroutineScope(context).async { read() }
    return withTimeoutOrNull(timeout) { reading.await() }.also { if (reading.isActive) reading.cancel() }
}

/** The display name of the first row of [cursor]; null without a row, without the column, or with a blank name. */
internal fun displayNameIn(cursor: Cursor): String? {
    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    if (column < 0 || !cursor.moveToFirst() || cursor.isNull(column)) return null
    return cursor.getString(column).takeIf { it.isNotBlank() }
}
