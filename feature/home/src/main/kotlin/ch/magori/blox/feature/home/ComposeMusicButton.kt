package ch.magori.blox.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines

/** The glyph of the button, a note and a pencil, drawn in a box of this size. */
private val GlyphSize = 18.dp
private const val GLYPH_STROKE = 0.1f

/**
 * « COMPOSER UNE MUSIQUE » (V1, P4.21): under the choice of music, framed in ink, a note and a pencil before its label
 * in capitals, on one line, or on two in a large font. TalkBack reads it as written, not letter by letter.
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
        // On one line, shrinking if needed, or on two in a large font, never cut: at 200 %, in the pixel font, one line
        // would hold the 20 letters only below the size of the text with the normal font; on two, « COMPOSER UNE » and
        // « MUSIQUE » stay larger. Two lines hold no word cut in the middle: « COMPOSER » alone would leave three.
        val style = MaterialTheme.typography.labelLarge
        Text(
            text = label.uppercase(),
            style = style,
            textAlign = TextAlign.Center,
            maxLines = 2,
            autoSize = shrinkToFewestLines(style.fontSize),
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
