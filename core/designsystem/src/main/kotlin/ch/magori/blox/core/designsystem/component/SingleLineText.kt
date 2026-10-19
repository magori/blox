package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import ch.magori.blox.core.designsystem.theme.BloxTypographies

/**
 * [text] on one line, in [style], shrunk step by step down to [BloxTypographies.MinimumFontSize] when it does not fit:
 * the pixel font of Original is wide, and the system font may be twice as large.
 */
@Composable
fun SingleLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontStyle: FontStyle? = null,
) {
    Text(
        text = text,
        color = color,
        style = style,
        fontStyle = fontStyle,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = BloxTypographies.MinimumFontSize, maxFontSize = style.fontSize),
        modifier = modifier,
    )
}
