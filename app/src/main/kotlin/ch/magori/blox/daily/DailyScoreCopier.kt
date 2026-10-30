package ch.magori.blox.daily

import android.os.Build
import ch.magori.blox.core.data.DailyResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * « Copier mon score » of the Graine du jour (docs/graine-du-jour.md), for its entry and the game over of its game:
 * puts in the [clipboard] the text of the [results] of a date ([dailyScoreText], in the [texts] of the application),
 * read when the copy is asked for, so that the try that just ended is in it (the game over shows once its try is
 * written). Results of another date are never copied under that date: nothing is copied, as without results. Below
 * Android 13, which shows nothing of a copy, the player is told by [confirm]; from Android 13 ([sdkInt]) the system
 * says it.
 *
 * The copy does not take the lock of the writes of the daily games: a game left, then a copy asked for at once on the
 * entry, may read the results from before the end of that game is written. It copies what the tiles of the entry show
 * at that moment, which they update once the end is written.
 *
 * Copies in [scope], which lives as long as the application, on the main thread: a copy is never lost with the screen
 * that asked for it.
 */
internal class DailyScoreCopier(
    private val results: Flow<DailyResults?>,
    private val texts: DailyScoreTexts,
    private val clipboard: (text: String) -> Unit,
    private val confirm: () -> Unit,
    private val sdkInt: Int,
    private val scope: CoroutineScope,
) {
    /** Copies the score of [date], the date of the entry shown or of the game over. */
    fun copy(date: LocalDate) {
        scope.launch {
            val ofDate = results.firstOrNull()?.takeIf { it.date == date } ?: return@launch
            clipboard(dailyScoreText(ofDate, texts))
            if (sdkInt < Build.VERSION_CODES.TIRAMISU) confirm()
        }
    }
}
