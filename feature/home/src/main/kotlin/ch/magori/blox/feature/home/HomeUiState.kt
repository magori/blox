package ch.magori.blox.feature.home

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice

/**
 * What the home screen shows (V1).
 *
 * @property theme the chosen theme.
 * @property music the chosen music.
 * @property startLevel the chosen start level, always one of [startLevels].
 * @property startLevels the levels the mode lets a game start at, in increasing order.
 * @property record the best score, its real value: the screen caps what it shows.
 * @property listenable the musics the player may listen to before playing, those with a BloX score.
 * @property previewing the music being listened to, one of [listenable]; null when none is.
 */
internal data class HomeUiState(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val startLevels: List<Int>,
    val record: Int,
    val listenable: List<MusicChoice>,
    val previewing: MusicChoice?,
)
