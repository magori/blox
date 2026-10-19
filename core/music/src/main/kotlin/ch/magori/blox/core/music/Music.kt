package ch.magori.blox.core.music

/**
 * A music the game may play: a BloX [Score], rendered note by note, or a [MusicFile] of the player, an audio
 * document read as it is. Sealed, so that a player that plays both treats each kind on purpose.
 */
public sealed interface Music

/**
 * An audio file the player chose on the device (« Mon fichier »), known by the [uri] of its document, as the
 * document picker of the system gives it (`content://…`). Kept as text, so that this module knows nothing of
 * Android. The file stays on the device: its uri is never backed up, and its permission does not follow the
 * player to another device.
 */
public data class MusicFile(public val uri: String) : Music {
    init {
        require(uri.isNotBlank()) { "A music file is known by the uri of its document" }
    }
}
