package ch.magori.blox.feature.home

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.data.MusicChoice

/**
 * What the music of the home asks for, its row in the card and the sheet Musique, each to its callback; no logic here
 * (the view model decides): choosing a music, listening to one or to the music of the player for the games, changing
 * the file of « Mon fichier », and closing the sheet.
 */
@Immutable
internal data class MusicActions(
    val onMusicChosen: (MusicChoice) -> Unit,
    val onListen: (MusicChoice) -> Unit,
    val onListenGameMusic: () -> Unit,
    val onChangeMusicFile: () -> Unit,
    val onSheetClosed: () -> Unit,
) {
    companion object {
        /** Music that does nothing: previews and screenshots. */
        val None = MusicActions({}, {}, {}, {}, {})
    }
}
