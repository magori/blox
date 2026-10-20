package ch.magori.blox.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The side of the grid of a stroke drawing. */
private const val STROKE_GRID = 24f

/** The thickness of the strokes, on the grid. */
private const val STROKE_WIDTH = 2.25f

/**
 * The strokes of [icon] as a vector: unfilled lines [STROKE_WIDTH] thick on a grid of [STROKE_GRID], rounded at their
 * ends and joins, in black, to be tinted.
 */
internal fun strokeIcon(icon: BloxIcon): ImageVector = ImageVector.Builder(
    name = icon.name,
    defaultWidth = STROKE_GRID.dp,
    defaultHeight = STROKE_GRID.dp,
    viewportWidth = STROKE_GRID,
    viewportHeight = STROKE_GRID,
).addPath(
    pathData = addPathNodes(icon.strokes),
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = STROKE_WIDTH,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).build()
