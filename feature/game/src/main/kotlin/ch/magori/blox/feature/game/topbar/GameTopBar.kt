package ch.magori.blox.feature.game.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines
import ch.magori.blox.core.designsystem.theme.shrinkToFit
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.PanelLabel
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.ValueText

/** The sizes of the top bar, in one place; the touch target is [BloxSizes.minTouchTarget]. */
private object TopBarSizes {
    /** Between the pause button, the title and the record. */
    val gap = 8.dp

    /** The pause glyph inside its button. */
    val glyph = 24.dp

    /** The mode title: CLASSIQUE, 9 letters of 1 em in the pixel font, fits beside a record of 6 digits on 360 dp. */
    val title = 14.sp
}

/**
 * The top bar of the game screen (view V2): the pause button, the mode title and the record. No
 * logic here: the record texts come from [topBarTexts], the glyph from [pauseBars], the rows from [topBarRows]. A
 * `null` [record] is not known yet: its value is hidden, in a place of the same size, so that nothing moves when it
 * comes. The pause button does nothing unless [pauseEnabled]: under « Agrandissez la fenêtre », the game is already
 * paused.
 */
@Composable
internal fun GameTopBar(
    record: Int?,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    pauseEnabled: Boolean = true,
) {
    val texts = topBarTexts(record ?: 0)
    val title = stringResource(R.string.feature_game_top_title).uppercase()
    val recordLabel = stringResource(R.string.feature_game_top_record)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        when (rememberTopBarRows(constraints.maxWidth, title, recordLabel, texts.record)) {
            TopBarRows.ONE -> Row(
                horizontalArrangement = Arrangement.spacedBy(TopBarSizes.gap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PauseButton(onPause = onPause, enabled = pauseEnabled)
                // On two lines, shrinking if needed: with an enlarged font, back to its size with the normal font,
                // where CLASSIQUE fits whole, rather than cut.
                ModeTitle(text = title, autoSize = shrinkToFit(TopBarSizes.title), modifier = Modifier.weight(1f))
                // At most as wide as the title: with an enlarged font, the record shrinks rather than the title.
                Record(record, texts, recordLabel, modifier = Modifier.weight(1f, fill = false))
            }

            // The title on the whole width, on one line if it holds there at its smallest size, on two otherwise:
            // beside the pause button, CLASSIQUE would need more than its half (P2.26).
            TopBarRows.TITLE_ABOVE -> Column(
                verticalArrangement = Arrangement.spacedBy(TopBarSizes.gap),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ModeTitle(
                    text = title,
                    autoSize = shrinkToFewestLines(TopBarSizes.title),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PauseButton(onPause = onPause, enabled = pauseEnabled)
                    // No larger than with the normal font: the height of the bar is taken from the board.
                    Record(
                        record = record,
                        texts = texts,
                        label = recordLabel,
                        capToNormalFont = true,
                        modifier = Modifier.padding(start = TopBarSizes.gap),
                    )
                }
            }
        }
    }
}

/**
 * The rows of the top bar [width] pixels wide ([topBarRows]): the room beside the pause button against the widths the
 * title and the record need at the smallest size they may shrink to ([shrinkFloor]), the longest word of the title,
 * and the wider of the record label and its value.
 */
@Composable
private fun rememberTopBarRows(
    width: Int,
    title: String,
    recordLabel: String,
    record: String,
): TopBarRows {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = BloxTheme.typography
    return remember(width, title, recordLabel, record, density, typography, measurer) {
        fun widthAtSmallestSize(
            text: AnnotatedString,
            style: TextStyle,
        ) = measurer.measure(
            text = text,
            style = style.copy(fontSize = density.shrinkFloor(style.fontSize)),
            maxLines = 1,
            softWrap = false,
        ).size.width.toFloat()

        val titleStyle = typography.title.copy(fontSize = TopBarSizes.title)
        val titleWidth = title.split(' ').maxOf { widthAtSmallestSize(AnnotatedString(it), titleStyle) }
        val labelStyle = typography.label.copy(fontSize = PanelSizes.label)
        val valueStyle = typography.value.copy(fontSize = PanelSizes.value)
        val recordWidth = maxOf(
            widthAtSmallestSize(AnnotatedString(recordLabel.uppercase()), labelStyle),
            widthAtSmallestSize(AnnotatedString(record), valueStyle),
        )
        // In whole pixels, as the row rounds the button and its gaps.
        val beside = with(density) { BloxSizes.minTouchTarget.roundToPx() + 2 * TopBarSizes.gap.roundToPx() }
        topBarRows(room = (width - beside).toFloat(), titleWidth = titleWidth, recordWidth = recordWidth)
    }
}

/** The mode title, centred, in bold, on two lines at most, shrinking by [autoSize]; TalkBack reads it as a heading. */
@Composable
private fun ModeTitle(
    text: String,
    autoSize: TextAutoSize,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = BloxTheme.palette.ink,
        style = BloxTheme.typography.title,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        autoSize = autoSize,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * The record, its [label] above its value from [texts], aligned on the right; TalkBack reads it as one, with the real
 * value. A `null` [record] keeps the place of its value, hidden. With [capToNormalFont], it is never larger than
 * with the normal font.
 */
@Composable
private fun Record(
    record: Int?,
    texts: TopBarTexts,
    label: String,
    modifier: Modifier = Modifier,
    capToNormalFont: Boolean = false,
) {
    val description = if (record == null) {
        label
    } else {
        stringResource(R.string.feature_game_value_description, label, texts.spokenRecord)
    }
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        PanelLabel(text = label, capToNormalFont = capToNormalFont)
        ValueText(
            text = texts.record,
            modifier = Modifier.alpha(if (record == null) 0f else 1f),
            capToNormalFont = capToNormalFont,
        )
    }
}

/**
 * The pause button: a drawn glyph, not a Unicode character, on a touch target of at least 48 dp, framed
 * like a key at rest ([pauseButtonColors]) so that it stands out from the page.
 */
@Composable
private fun PauseButton(
    onPause: () -> Unit,
    enabled: Boolean,
) {
    val colors = pauseButtonColors(BloxTheme.palette)
    val description = stringResource(R.string.feature_game_top_pause)
    val shape = BloxTheme.shapes.md
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // Read by the measured journey of `:benchmark` (`By.res`), whatever the language.
            .testTag("game_pause")
            .sizeIn(minWidth = BloxSizes.minTouchTarget, minHeight = BloxSizes.minTouchTarget)
            .clip(shape)
            .background(colors.background)
            .border(BloxBorders.frame, colors.outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onPause)
            .semantics { contentDescription = description },
    ) {
        val ink = colors.ink
        Spacer(
            modifier = Modifier
                .size(TopBarSizes.glyph)
                .drawWithCache {
                    val bars = pauseBars(size)
                    onDrawBehind { bars.forEach { bar -> drawRect(ink, topLeft = bar.topLeft, size = bar.size) } }
                },
        )
    }
}

@Composable
private fun TopBarPreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        GameTopBar(
            record = 123_450,
            onPause = {},
            modifier = Modifier
                .width(360.dp)
                .background(BloxTheme.palette.page)
                .padding(TopBarSizes.gap),
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360)
@Composable
private fun GameTopBarOriginalPreview() {
    TopBarPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360)
@Composable
private fun GameTopBarColorfulPreview() {
    TopBarPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, fontScale = 2f)
@Composable
internal fun GameTopBarLargeFontPreview() {
    TopBarPreview(BloxThemeKind.ORIGINAL)
}
