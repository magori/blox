package ch.magori.blox.music

import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The name of a music file, read from its provider as the picker of the system shows it, and never a failure: a
 * document gone, out of reach, or without a name has none.
 */
// Sentences in @DisplayName: the dex refuses spaces in method names below API 30 (minSdk 26).
internal class DocumentMusicFileNamesTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val names = DocumentMusicFileNames(context.contentResolver)

    @Test
    @DisplayName("the name of an audio file of the device is its display name, and it has none once deleted")
    fun theNameOfAnAudioFileIsItsDisplayName() {
        // Given an audio file of the application in the media of the device (no permission asked from Android 10)
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "orage-d-ete.ogg")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/ogg")
        }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values))
        val file = MusicFile(uri.toString())
        try {
            // When / Then
            assertThat(runBlocking { names.nameOf(file) }).isEqualTo("orage-d-ete.ogg")
        } finally {
            resolver.delete(uri, null, null)
        }

        // And once deleted
        assertThat(runBlocking { names.nameOf(file) }).isNull()
    }

    @Test
    @DisplayName("a document out of reach, its permission lost or never given, has no name")
    fun aDocumentOutOfReachHasNoName() {
        // Given a document of another provider, which never gave this application a permission on it
        val file = MusicFile("content://com.android.externalstorage.documents/document/primary%3AMusic%2Fa.ogg")

        // When / Then
        assertThat(runBlocking { names.nameOf(file) }).isNull()
    }

    @Test
    @DisplayName("a document of no provider, or no document at all, has no name")
    fun aDocumentOfNoProviderHasNoName() {
        assertThat(runBlocking { names.nameOf(MusicFile("content://ch.magori.blox.nowhere/audio/1")) }).isNull()
        assertThat(runBlocking { names.nameOf(MusicFile("not a uri")) }).isNull()
    }

    @Test
    @DisplayName("the name is the display name of the first row; none without a row, a column, or with a blank one")
    fun theNameIsTheDisplayNameOfTheFirstRow() {
        assertThat(displayNameIn(cursorOf("orage-d-ete.ogg"))).isEqualTo("orage-d-ete.ogg")
        assertThat(displayNameIn(cursorOf())).isNull()
        assertThat(displayNameIn(cursorOf(null))).isNull()
        assertThat(displayNameIn(cursorOf(" "))).isNull()
        assertThat(displayNameIn(MatrixCursor(arrayOf(OpenableColumns.SIZE)).apply { addRow(arrayOf(42)) })).isNull()
    }

    /** A cursor with a row per name of [names], under the display name of the documents. */
    private fun cursorOf(vararg names: String?) =
        MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { names.forEach { addRow(arrayOf(it)) } }
}
