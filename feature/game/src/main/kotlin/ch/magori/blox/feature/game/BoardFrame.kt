package ch.magori.blox.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.feature.game.feedback.FeedbackEffects
import ch.magori.blox.feature.game.feedback.InsetStroke
import ch.magori.blox.feature.game.feedback.boardFrameBorder

/** The part of the frame the iridescent reflection of four lines covers as it goes around. */
private const val IRIS_REFLECTION = 0.25f

/**
 * The frame of the board (views V3, V28), drawn under its cells: a block of `board` framed by 2 dp of `panel-border`,
 * corners `radius-md` (square in Original). Its size, around the cells, comes from [GameLayout.boardFrame].
 *
 * With [effects] (Couleurs), read only while drawing (ADR 0024): while the stack is in danger, a frame of `danger` as
 * [boardFrameBorder] says; once four lines are cleared, an iridescent reflection that goes around it, as thick, or in
 * reduced effects the whole frame iridescent, still.
 */
@Composable
internal fun BoardFrame(
    modifier: Modifier = Modifier,
    effects: FeedbackEffects? = null,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    val rest = boardFrameBorder(palette, inDanger = false)
    Spacer(
        modifier = modifier
            // First, so as to draw over the frame drawn after it.
            .then(if (effects == null) Modifier else Modifier.frameEffects(effects, palette, shape))
            .background(palette.boardBackground, shape)
            .border(rest.width, rest.color, shape),
    )
}

/** The danger and the iridescent border of the frame, drawn over it, as [effects] say on each frame. */
private fun Modifier.frameEffects(
    effects: FeedbackEffects,
    palette: BloxPalette,
    shape: Shape,
): Modifier = drawWithCache {
    val danger = boardFrameBorder(palette, inDanger = true)
    val stroke = InsetStroke(shape, danger.width.toPx(), this)
    val measure = PathMeasure().apply { setPath(stroke.path, forceClosed = true) }
    val length = measure.length
    val reflection = Path()
    val iris = Brush.sweepGradient(
        palette.iris.let { listOf(it.start, it.middle, it.end, it.start) },
        center = Offset(size.width / 2, size.height / 2),
    )
    onDrawWithContent {
        drawContent()
        if (effects.inDanger()) stroke.inside(this) { drawPath(stroke.path, danger.color, style = stroke.style) }
        val border = effects.irisBorder() ?: return@onDrawWithContent
        stroke.inside(this) {
            if (border.whole) {
                drawPath(stroke.path, iris, alpha = border.alpha, style = stroke.style)
            } else {
                // From its head, a part of the frame, going on from its start once past its end.
                val start = border.head * length
                val end = start + IRIS_REFLECTION * length
                reflection.reset()
                measure.getSegment(start, end, reflection, startWithMoveTo = true)
                if (end > length) measure.getSegment(0f, end - length, reflection, startWithMoveTo = true)
                drawPath(reflection, iris, alpha = border.alpha, style = stroke.style)
            }
        }
    }
}
