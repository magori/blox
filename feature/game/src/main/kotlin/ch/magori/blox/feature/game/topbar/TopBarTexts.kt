package ch.magori.blox.feature.game.topbar

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.sidepanel.scoreText

/** The texts of the top bar: the [best] shown, and the [spokenBest] read by TalkBack. */
@Immutable
internal data class TopBarTexts(val best: String, val spokenBest: String)

/**
 * The texts of the top bar for [best]: shown as the score in the game, on 6 digits and capped at
 * 999 999; read by TalkBack as the real value, without padding zeros nor cap.
 */
internal fun topBarTexts(best: Int): TopBarTexts = TopBarTexts(
    best = scoreText(Points(best)),
    spokenBest = best.toString(),
)
