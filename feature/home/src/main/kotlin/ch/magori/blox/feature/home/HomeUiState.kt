package ch.magori.blox.feature.home

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice

/**
 * What the home screen shows (V27).
 *
 * @property theme the chosen theme.
 * @property music the music chosen on the home screen, the one the games play when no [gameMusic] is chosen.
 * @property startLevel the chosen start level, always one of [startLevels].
 * @property startLevels the levels the mode lets a game start at, one after the other: the gauge offers each of them.
 * @property record the best score, its real value: the screen caps what it shows.
 * @property listenable the musics the player may listen to before playing: those with a BloX score, and « Mon
 *   fichier » once a music file is picked.
 * @property previewing the music being listened to, one of [listenable]; null when none is.
 * @property hasMusicFile whether a music file is picked for « Mon fichier »: it may then be changed.
 * @property musicFileName the name of the music file picked, shown under « Mon fichier »; null when there is none, or
 *   when it is not known (a file picked before the names were kept, a name that could not be read).
 * @property gameMusic the music of the player played during the games (« Mes musiques »), instead of [music]; null
 *   when there is none.
 */
internal data class HomeUiState(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val startLevels: IntRange,
    val record: Int,
    val listenable: List<MusicChoice>,
    val previewing: MusicChoice?,
    val hasMusicFile: Boolean = false,
    val musicFileName: String? = null,
    val gameMusic: GameMusicUi? = null,
) {
    /**
     * The music choice shown selected: [music], unless a music of the player is played during the games, then the only
     * music selected.
     */
    val selectedMusic: MusicChoice? get() = music.takeIf { gameMusic == null }

    /**
     * The music choice the reminder under Jouer names, the one the game will play: [music], unless « Mon fichier » has
     * no file, when the game plays [MusicChoice.MY_FILE_FALLBACK] in its place.
     */
    val remindedMusic: MusicChoice
        get() = if (music == MusicChoice.MY_FILE && !hasMusicFile) MusicChoice.MY_FILE_FALLBACK else music
}

/**
 * The music of the player played during the games, shown among the music choices of the home screen: its [name], and
 * whether it is being listened to, [previewed].
 */
internal data class GameMusicUi(val name: String, val previewed: Boolean)
