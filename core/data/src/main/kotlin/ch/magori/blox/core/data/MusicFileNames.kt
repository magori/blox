package ch.magori.blox.core.data

import ch.magori.blox.core.music.MusicFile

/**
 * Reads the name of a music file of the player as the picker of the system shows it (« orage-d-ete.ogg »), once, when
 * the file is chosen: it is then kept with the file ([MusicFileRepository]), so that the home never asks the document
 * again, nor loses the name with a permission.
 */
fun interface MusicFileNames {
    /** The name of [file]; null when it cannot be read (no such document, no permission, no name). Never fails. */
    suspend fun nameOf(file: MusicFile): String?
}
