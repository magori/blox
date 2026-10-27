package ch.magori.blox.feature.game.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import ch.magori.blox.core.board.BoardEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.feature.game.controls.ControlButton

/**
 * The effects of the game screen, read from the [feedback] of the game each time they are drawn (ADR 0024), never in
 * a composition: [reduced], as the theme says (Effets réduits), nothing moves. One instance for the screen, so that the
 * board keeps its painters as the effects play.
 */
@Stable
internal class FeedbackEffects(private val feedback: () -> GameFeedback, private val reduced: Boolean) {
    /** The effects on the cells of the board: the light of a lock, the clear of full rows, the rows that slide down. */
    val board: BoardEffects = object : BoardEffects {
        override fun rowLift(row: Int): Float = feedback().rowLift(row, reduced)

        override fun rowGlows(row: Int): Boolean = feedback().rowGlows(row)

        override fun cellScale(position: Position): Float = feedback().cellScale(position, reduced)

        override fun cellLight(position: Position): Float = feedback().lockLight(position, reduced)
    }

    /** The angle of the icon of the key [button], in degrees, clockwise. */
    fun keyAngle(button: ControlButton): Float = feedback().keyAngle(button, reduced)

    /** The sticker « +points » on this tick, null when none shows. */
    fun sticker(): StickerLook? = feedback().sticker(reduced)

    /** The score shown for the [score] of the game, which counts up once rows are cleared. */
    fun shownScore(score: Points): Points = feedback().shownScore(score, reduced)
}

/**
 * The effects of the game whose feedback [feedback] gives, with the effects of the theme; none in Original, which is
 * kept as it was, without the feedback of Couleurs.
 */
@Composable
internal fun rememberFeedbackEffects(feedback: () -> GameFeedback): FeedbackEffects? {
    val reduced = BloxTheme.effects.reduced
    val shown = BloxTheme.kind != BloxThemeKind.ORIGINAL
    return remember(feedback, reduced, shown) { if (shown) FeedbackEffects(feedback, reduced) else null }
}
