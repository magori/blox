package ch.magori.blox.core.data

/**
 * A music [MusicLibrary.delete] took out of the list, kept by the screen until its next action so that « Annuler »
 * may give it back ([MusicLibrary.restore]): the [music] as it was saved, its [place] in the list, and whether it
 * [wasGameMusic], the music played during the games.
 */
data class DeletedMusic(val music: SavedMusic, val place: Int, val wasGameMusic: Boolean)
