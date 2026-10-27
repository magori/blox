package ch.magori.blox.feature.game.pause

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * This part, of [rows] rows, drawn as the curtain of the pause given by [curtain]: only the rows it covers, from the
 * top, at its opacity; nothing without a curtain. The curtain is read only while drawing (ADR 0024): it moves
 * without recomposing anything. TalkBack reads nothing of it.
 */
internal fun Modifier.drawnAsCurtain(
    curtain: () -> PauseCurtain?,
    rows: Int,
): Modifier = clearAndSetSemantics {}
    .graphicsLayer { alpha = curtain()?.opacity ?: 0f }
    .drawWithContent {
        val covered = curtain()?.coveredRows(rows) ?: 0
        if (covered > 0) clipRect(bottom = size.height * covered / rows) { this@drawWithContent.drawContent() }
    }

/**
 * Over this part, of [rows] rows, the curtain of the pause given by [curtain], in [color], the background under the
 * part: it hides the rows it covers, from the top. The curtain is read only while drawing (ADR 0024).
 */
internal fun Modifier.hiddenByCurtain(
    curtain: () -> PauseCurtain?,
    rows: Int,
    color: Color,
): Modifier = drawWithContent {
    drawContent()
    val shown = curtain() ?: return@drawWithContent
    val covered = shown.coveredRows(rows)
    if (covered > 0) drawRect(color, size = Size(size.width, size.height * covered / rows), alpha = shown.opacity)
}
