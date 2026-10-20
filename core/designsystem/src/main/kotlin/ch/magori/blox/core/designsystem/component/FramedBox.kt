package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.designsystem.theme.shrinkingLineHeight

/**
 * The body of a button framed as a panel: 48 dp high at least, on [background], framed in [frame], its [content]
 * centred, [labelPadding] more on each side of it when the button is as wide as its label. What it does, and how
 * TalkBack reads it, is up to [modifier].
 */
@Composable
fun FramedBox(
    background: Color,
    frame: Color,
    modifier: Modifier = Modifier,
    labelPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val shape = BloxTheme.shapes.button
    Box(
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .background(background, shape)
            .border(BloxBorders.frame, frame, shape)
            .padding(horizontal = framedBoxPadding(labelPadding)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The label of a [FramedBox], [text] in [color], on as many lines as it needs rather than shrunk, going to the line
 * between its words only: a word too wide for the box shrinks, down to [shrinkFloor], rather than be cut.
 */
@Composable
fun WrappingLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val style = wrappingLabelStyle
    Text(
        text = text,
        color = color,
        style = style,
        textAlign = TextAlign.Center,
        autoSize = shrinkToWholeWords(style.fontSize),
        modifier = modifier.padding(vertical = 4.dp),
    )
}

/**
 * Between each side of a [FramedBox] and its content, given its [labelPadding]: its frame, one more dp, then the
 * padding; in pixels, [framedBoxPaddingPx].
 */
internal fun framedBoxPadding(labelPadding: Dp): Dp = BloxBorders.frame + 1.dp + labelPadding

/** The style of a [WrappingLabel]; the answers of a window measure their words with it. */
internal val wrappingLabelStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography.labelMedium.shrinkingLineHeight()
