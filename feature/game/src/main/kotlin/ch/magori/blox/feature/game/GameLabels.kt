package ch.magori.blox.feature.game

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * What the game screen says of the variant of its game (ADR 0026), given by its setup ([GameVariants.setupOf]): the
 * [title] of the top bar, the label of the [best] to beat beside it, the sticker [newBest] once it is beaten, the badge
 * [gameOverNewBest] of the game over when the score beat it, and whether the game over offers to copy the score
 * ([copyOffered]). The screen holds no `when (variant)`: it shows these.
 */
@Immutable
internal data class GameLabels(
    @param:StringRes val title: Int,
    @param:StringRes val best: Int,
    @param:StringRes val newBest: Int,
    @param:StringRes val gameOverNewBest: Int,
    val copyOffered: Boolean,
) {
    companion object {
        /** The Classic: MODE CLASSIQUE, its record, NOUVEAU RECORD; nothing to copy. */
        val Classic = GameLabels(
            title = R.string.feature_game_top_title,
            best = R.string.feature_game_top_record,
            newBest = R.string.feature_game_top_new_record,
            gameOverNewBest = R.string.feature_game_over_new_record,
            copyOffered = false,
        )

        /**
         * The daily game (V39): GRAINE DU JOUR, the best of the day, NOUVEAU MEILLEUR once beaten in the game, MEILLEUR
         * DU JOUR at its game over; Copier.
         */
        val Daily = GameLabels(
            title = R.string.feature_game_daily_title,
            best = R.string.feature_game_daily_best,
            newBest = R.string.feature_game_daily_top_new_best,
            gameOverNewBest = R.string.feature_game_daily_new_best,
            copyOffered = true,
        )
    }
}
