package ch.magori.blox.feature.home

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice

/**
 * What the home screen shows (V1).
 *
 * @property theme the chosen theme.
 * @property music the music chosen on the home screen, the one the games play when no [gameMusic] is chosen.
 * @property startLevel the chosen start level, always one of [startLevels].
 * @property startLevels the levels the mode lets a game start at, in increasing order.
 * @property record the best score, its real value: the screen caps what it shows.
 * @property listenable the musics the player may listen to before playing: those with a BloX score, and « Mon
 *   fichier » once a music file is picked.
 * @property previewing the music being listened to, one of [listenable]; null when none is.
 * @property hasMusicFile whether a music file is picked for « Mon fichier »: it may then be changed.
 * @property gameMusic the music of the player played during the games (« Mes musiques »), instead of [music]; null
 *   when there is none.
 * @property vibrations whether the games vibrate: off unless the player turned them on.
 * @property soundEffects whether the games play their sound effects: off unless the player turned them on.
 */
internal data class HomeUiState(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val startLevels: List<Int>,
    val record: Int,
    val listenable: List<MusicChoice>,
    val previewing: MusicChoice?,
    val hasMusicFile: Boolean = false,
    val gameMusic: GameMusicUi? = null,
    val vibrations: Boolean = false,
    val soundEffects: Boolean = false,
) {
    /**
     * The music choice shown selected: [music], unless a music of the player is played during the games, then the only
     * music selected.
     */
    val selectedMusic: MusicChoice? get() = music.takeIf { gameMusic == null }

    /**
     * The [startLevels] in rows of [START_LEVELS_PER_ROW], in order: 0 to 9 on two rows of five, so that each cell is
     * 48 dp wide at least on a phone of 360 dp. The last row holds those left, fewer if their number is not a multiple.
     */
    val startLevelRows: List<List<Int>> get() = startLevels.chunked(START_LEVELS_PER_ROW)
}

/**
 * The music of the player played during the games, shown among the music choices of the home screen: its [name], and
 * whether it is being listened to, [previewed].
 */
internal data class GameMusicUi(val name: String, val previewed: Boolean)

/** The start levels on a row of the home screen: five cells of 48 dp at least side by side on 360 dp. */
internal const val START_LEVELS_PER_ROW = 5
