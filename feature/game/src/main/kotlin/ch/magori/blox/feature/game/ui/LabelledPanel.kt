package ch.magori.blox.feature.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The sizes shared by the panels and values of the game screens, in one place; their frame is `BloxBorders.frame`. */
internal object PanelSizes {
    /** Inside a panel, around its content. */
    val padding = 8.dp

    /** Between the label and the content of a panel. */
    val labelGap = 4.dp
    val label = 11.sp

    /** The largest size of a value; it shrinks to stay on one line. */
    val value = 18.sp
}

/**
 * A panel of the game screens: the theme panel background, frame and shape, its [label] in capitals
 * above its [content]. TalkBack reads it as one [description].
 */
@Composable
internal fun LabelledPanel(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(PanelSizes.padding),
        verticalArrangement = Arrangement.spacedBy(PanelSizes.labelGap),
        horizontalAlignment = contentAlignment,
    ) {
        PanelLabel(text = label)
        content()
    }
}

/**
 * A value of the game screens: in the value font of the theme, every digit as wide as the others, on one
 * line, shrinking from [maxFontSize] if needed, down to [shrinkFloor].
 */
@Composable
internal fun ValueText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = PanelSizes.value,
) {
    val typography = BloxTheme.typography
    Text(
        text = typography.valueText(text),
        modifier = modifier,
        color = BloxTheme.palette.ink,
        style = typography.value,
        maxLines = 1,
        softWrap = false,
        autoSize = shrinkToFit(maxFontSize),
    )
}

/**
 * The label of a panel or of a value: in capitals, in the theme label colour and font, on one line, shrinking
 * if needed, down to [shrinkFloor]: with an enlarged font, « SUIVANTE » stays whole.
 */
@Composable
internal fun PanelLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = BloxTheme.palette.label,
        style = BloxTheme.typography.label,
        maxLines = 1,
        softWrap = false,
        autoSize = shrinkToFit(PanelSizes.label),
    )
}
