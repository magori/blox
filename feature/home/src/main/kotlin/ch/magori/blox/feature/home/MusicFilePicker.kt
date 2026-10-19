package ch.magori.blox.feature.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** The documents the picker offers for « Mon fichier »: every audio file. */
private val AUDIO_DOCUMENTS = arrayOf("audio/*")

/**
 * The picker of the system for « Mon fichier » (`ACTION_OPEN_DOCUMENT`, audio files kept on the device only): the
 * function it gives opens it. The document picked is kept readable across restarts (a persistable permission), the
 * permission of the file picked before given back, then its uri goes to [onPicked]; a picker cancelled tells nothing,
 * so that the music chosen before stays. A device without a document picker opens nothing. Kept without logic: when
 * it opens, and what the uri means, are decided by the view model, tested.
 */
@Composable
internal fun rememberMusicFilePicker(onPicked: (uri: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(OpenLocalDocument()) { uri ->
        if (uri != null) {
            keepOnlyReadable(context, uri)
            onPicked(uri.toString())
        }
    }
    return {
        try {
            launcher.launch(AUDIO_DOCUMENTS)
        } catch (_: ActivityNotFoundException) {
            // No document picker on this device: « Mon fichier » cannot be chosen, the choice stays.
        }
    }
}

/**
 * The document picker, offering only the documents kept on the device ([Intent.EXTRA_LOCAL_ONLY]): a file in a cloud
 * would be downloaded on each game, or not at all without a network.
 */
private class OpenLocalDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(
        context: Context,
        input: Array<String>,
    ): Intent = super.createIntent(context, input).putExtra(Intent.EXTRA_LOCAL_ONLY, true)
}

/**
 * Keeps [uri] readable after a restart of the application, and gives back the permissions kept for the files picked
 * before, which the system counts: only one file is « Mon fichier ». A provider that refuses a lasting permission
 * leaves the document readable until then only: the game then falls back on Korobeiniki.
 */
private fun keepOnlyReadable(
    context: Context,
    uri: Uri,
) {
    val resolver = context.contentResolver
    try {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Readable for now only.
    }
    resolver.persistedUriPermissions.filter { it.uri != uri }.forEach { before ->
        try {
            resolver.releasePersistableUriPermission(before.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Given back already.
        }
    }
}
