package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import ch.magori.blox.core.board.garden.GardenCellEffects
import ch.magori.blox.core.designsystem.frame.rememberEffectTick
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.game.model.Position

/**
 * What moves on the grid (docs/jardin-musical.md, Retours; ADR 0024): the [feedback] of the last change of the garden,
 * at its [tick], and the bounce of the notes of the column played, at its [bounceTick], both read in the drawing alone.
 */
@Stable
internal class GardenMotion(val feedback: GardenFeedback?, val tick: () -> Int, val bounceTick: () -> Int)

/**
 * The motion of the grid of [state], counted on the frames of the screen in the effects of the theme: the feedback from
 * its start, the change shown first, as on coming back to the screen, being over already; the bounce from each column
 * the play head comes onto. Both [frozen] at a tick for the previews.
 */
@Composable
internal fun rememberGardenMotion(
    state: GardenUiState?,
    frozen: Int?,
): GardenMotion {
    val effects = BloxTheme.effects
    val feedback = remember(state) { state?.let(::gardenFeedbackOf) }
    val tick = rememberEffectTick(
        key = feedback,
        move = feedback?.move,
        frozen = frozen,
        length = feedback?.ticks(effects) ?: 0,
    )
    val column = state?.playedColumn
    val bounceTick = rememberEffectTick(
        key = column,
        start = frozen ?: if (column == null) Int.MAX_VALUE else 0,
        length = if (frozen != null || effects.reduced) 0 else BloxMotion.gardenBounce,
    )
    return remember(feedback, tick, bounceTick) { GardenMotion(feedback, tick, bounceTick) }
}

/**
 * The effects of the note on [position] as [motion] says, in [effects], read in the drawing alone: the feedback of the
 * last change; the bounce, if it is [played].
 */
internal class NoteEffects(
    private val motion: GardenMotion,
    private val effects: BloxEffects,
    private val position: Position,
    private val played: Boolean,
) : GardenCellEffects {
    override fun shown(): Float = motion.feedback?.shown(position, motion.tick(), effects) ?: 1f

    override fun lift(): Float = motion.feedback?.lift(position, motion.tick(), effects) ?: 0f

    override fun scale(): Float = if (played) playHeadBounce(motion.bounceTick(), effects) else 1f

    override fun light(): Float = motion.feedback?.light(position, motion.tick(), effects) ?: 0f
}
