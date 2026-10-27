package ch.magori.blox.feature.daily

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.PieceType
import java.time.LocalDate

/**
 * The entry of the Graine du jour (V38) on [date], the local date of the phone: the [firstPieces] of its seed, the
 * [startLevel] the daily game imposes, the [tries] of that date, its [firstTry] and its [best]. [firstTry] is null until
 * the first try is over, [best] until a try gave points or the first try is over: a try under way shows « — ».
 */
@Immutable
internal data class DailyUiState(
    val date: LocalDate,
    val firstPieces: List<PieceType>,
    val startLevel: Int,
    val tries: Int,
    val firstTry: Int?,
    val best: Int?,
) {
    /** Whether « Copier mon score » is offered: once a try of the day started. */
    val canCopy: Boolean get() = tries > 0
}
