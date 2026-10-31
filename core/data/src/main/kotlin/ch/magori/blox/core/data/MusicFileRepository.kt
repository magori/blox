package ch.magori.blox.core.data

import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.flow.Flow

/**
 * The audio file the player chose on the device for « Mon fichier », kept apart from the settings: its uri names a
 * document of the player and holds a permission of this device only, so it is never backed up nor moved to another
 * device; its name, read once when it is chosen, neither. The choice of « Mon fichier » itself stays in the settings
 * ([MusicChoice.MY_FILE]). A write fails with an [java.io.IOException] if the file cannot be written; any other
 * failure is a bug.
 */
interface MusicFileRepository {
    /**
     * The music file chosen; null until the player chooses one. Emits each change once.
     *
     * Never fails with an [java.io.IOException]: a file that cannot be read gives null, once, and the flow then ends.
     * Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val musicFile: Flow<MusicFile?>

    /** The music file chosen with its name, as [musicFile] gives it: the name and its file always go together. */
    val namedMusicFile: Flow<NamedMusicFile?>

    /**
     * Keeps [file] as the music file of the player, in place of the one before, with its [name], null when it is
     * unknown.
     *
     * @throws java.io.IOException if the file cannot be written.
     */
    suspend fun setMusicFile(
        file: MusicFile,
        name: String?,
    )
}

/**
 * A music [file] of the player and its [name], as the picker of the system showed it (« orage-d-ete.ogg »), read once
 * when the file was chosen; null when it is not known: a file chosen before the names were kept, or whose provider
 * gave none.
 */
data class NamedMusicFile(val file: MusicFile, val name: String?)
