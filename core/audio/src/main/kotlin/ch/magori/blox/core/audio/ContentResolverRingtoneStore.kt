package ch.magori.blox.core.audio

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.IOException
import java.io.OutputStream

/**
 * The ringtones of the shared storage, in the audio collection of the primary volume of `MediaStore` (ADR 0031), which
 * an application writes to without any permission from Android 10 on.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class ContentResolverRingtoneStore(private val resolver: ContentResolver) : RingtoneStore<Uri> {
    private val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    override fun insert(
        fileName: String,
        kind: RingtoneKind,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, WAV_MIME_TYPE)
            when (kind) {
                RingtoneKind.RINGTONE -> {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_RINGTONES}/$FOLDER/")
                    put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                }

                RingtoneKind.ALARM -> {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_ALARMS}/$FOLDER/")
                    put(MediaStore.Audio.Media.IS_ALARM, 1)
                }
            }
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        return resolver.insert(collection, values) ?: throw IOException("MediaStore made no entry for $fileName")
    }

    override fun output(entry: Uri): OutputStream =
        resolver.openOutputStream(entry) ?: throw IOException("MediaStore opened no stream for $entry")

    override fun publish(entry: Uri) {
        val updated =
            resolver.update(entry, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        if (updated != 1) throw IOException("MediaStore published $updated entries for $entry")
    }

    override fun delete(entry: Uri) {
        resolver.delete(entry, null, null)
    }

    private companion object {
        /** The type Android gives the `wav` extension. */
        const val WAV_MIME_TYPE = "audio/x-wav"

        /** The folder of BloX, in `Ringtones/` and in `Alarms/`. */
        const val FOLDER = "BloX"
    }
}
