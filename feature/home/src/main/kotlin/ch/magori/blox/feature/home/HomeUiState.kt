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
 */
internal data class HomeUiState(
    val theme: ThemeChoice,
    val music: MusicChoice,
    val startLevel: Int,
    val startLevels: List<Int>,
    val record: Int,
)
