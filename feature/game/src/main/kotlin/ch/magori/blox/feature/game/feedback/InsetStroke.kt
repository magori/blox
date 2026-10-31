package ch.magori.blox.feature.game.feedback

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

/**
 * A stroke [width] pixels wide along the inside of the edge of [shape], as `Modifier.border` draws it, for a drawing of
 * the size of [scope]: its [path], laid out once in the cache of the drawing, is drawn in [style] within [inside], so
 * that the stroke stays within the bounds.
 */
internal class InsetStroke(shape: Shape, private val width: Float, scope: CacheDrawScope) {
    val style: Stroke = Stroke(width)

    val path: Path = Path().apply {
        val size = Size((scope.size.width - width).coerceAtLeast(0f), (scope.size.height - width).coerceAtLeast(0f))
        addOutline(shape.createOutline(size, scope.layoutDirection, scope))
    }

    /** Runs [block] on [scope] moved to where [path] lies. */
    fun inside(
        scope: DrawScope,
        block: DrawScope.() -> Unit,
    ) {
        scope.translate(width / 2, width / 2, block)
    }
}
