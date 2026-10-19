package ch.magori.blox.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The glyph of the button, a note and a pencil, drawn in a box of this size. */
private val GlyphSize = 18.dp
private const val GLYPH_STROKE = 0.1f

/**
 * « COMPOSER UNE MUSIQUE » (V1, P4.21): under the choice of music, framed in ink, a note and a pencil before its label
 * in capitals, on one line. TalkBack reads it as written, not letter by letter.
 */
@Composable
internal fun ComposeMusicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = BloxTheme.palette.ink
    val label = stringResource(R.string.feature_home_compose)
    OutlinedButton(
        onClick = onClick,
        shape = BloxTheme.shapes.button,
        border = BorderStroke(BloxBorders.frame, ink),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = label },
    ) {
        Spacer(modifier = Modifier.size(GlyphSize).drawBehind { drawNoteAndPencil(ink) })
        SingleLineText(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** An eighth note on the left, a pencil slanting on the right. */
private fun DrawScope.drawNoteAndPencil(color: Color) {
    val stroke = size.width * GLYPH_STROKE
    // The note: its head, its stem, its flag.
    drawOval(color, topLeft = Offset(0f, size.height * 0.62f), size = Size(size.width * 0.34f, size.height * 0.28f))
    drawLine(
        color,
        Offset(size.width * 0.3f, size.height * 0.76f),
        Offset(size.width * 0.3f, size.height * 0.08f),
        stroke,
    )
    drawLine(
        color,
        Offset(size.width * 0.3f, size.height * 0.08f),
        Offset(size.width * 0.5f, size.height * 0.3f),
        stroke,
    )
    // The pencil: its body, then its tip.
    drawLine(
        color,
        Offset(size.width * 0.66f, size.height * 0.82f),
        Offset(size.width * 0.96f, size.height * 0.32f),
        strokeWidth = stroke * 2,
        cap = StrokeCap.Butt,
    )
    drawLine(color, Offset(size.width * 0.62f, size.height), Offset(size.width * 0.66f, size.height * 0.86f), stroke)
}
