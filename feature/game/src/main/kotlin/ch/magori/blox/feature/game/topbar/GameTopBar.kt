package ch.magori.blox.feature.game.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.notEnlarged
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines
import ch.magori.blox.core.designsystem.theme.shrinkToFit
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.PanelLabel
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.STICKER_TILT
import ch.magori.blox.feature.game.ui.ValueText
import ch.magori.blox.feature.game.ui.valueDescription

/** The sizes of the top bar, in one place; the touch target is [BloxSizes.minTouchTarget]. */
private object TopBarSizes {
    /** Between the pause button, the title and the record. */
    val gap = 8.dp

    /**
     * The mode title where the theme gives no size (Original): CLASSIQUE, 9 letters of 1 em in the pixel font, fits
     * beside a record of 6 digits on 360 dp. Couleurs takes `game-title`.
     */
    val title = 14.sp

    /** Around the text of the sticker NOUVEAU RECORD: as high as the label RECORD it replaces, give or take. */
    val stickerPaddingX = 4.dp
    val stickerPaddingY = 2.dp
}

/**
 * The top bar of the game screen (views V2, V3, V28): the pause button, a disc of the family Icône, the mode title
 * and the [record]. No logic here: the record texts come from [topBarTexts], the glyph from [pauseBars], the rows from
 * [topBarRows]. A record whose value is unknown is hidden, in a place of the same size, so that nothing moves when it
 * comes; once beaten, the sticker NOUVEAU RECORD takes the place of its label, the score its value, in the same place
 * of the same size. The pause button does nothing unless [pauseEnabled]: under « Agrandissez la fenêtre », the game is
 * already paused.
 */
@Composable
internal fun GameTopBar(
    record: ShownRecord,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    pauseEnabled: Boolean = true,
) {
    val texts = topBarTexts(record.value ?: 0)
    val title = stringResource(R.string.feature_game_top_title).uppercase()
    val labels = RecordLabels(
        record = stringResource(R.string.feature_game_top_record),
        newRecord = stringResource(R.string.feature_game_top_new_record),
    )
    val titleSize = BloxTheme.typography.gameTitle.fontSize.takeOrElse { TopBarSizes.title }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        when (rememberTopBarRows(constraints.maxWidth, title, titleSize, labels, texts.record)) {
            TopBarRows.ONE -> Row(
                horizontalArrangement = Arrangement.spacedBy(TopBarSizes.gap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PauseButton(onPause = onPause, enabled = pauseEnabled)
                // On two lines, shrinking if needed: with an enlarged font, back to its size with the normal font,
                // where CLASSIQUE fits whole, rather than cut.
                ModeTitle(text = title, autoSize = shrinkToFit(titleSize), modifier = Modifier.weight(1f))
                // At most as wide as the title: with an enlarged font, the record shrinks rather than the title.
                Record(record, texts, labels, modifier = Modifier.weight(1f, fill = false))
            }

            // The title on the whole width, on one line if it holds there at its smallest size, on two otherwise:
            // beside the pause button, CLASSIQUE would need more than its half (P2.26).
            TopBarRows.TITLE_ABOVE -> Column(
                verticalArrangement = Arrangement.spacedBy(TopBarSizes.gap),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ModeTitle(
                    text = title,
                    autoSize = shrinkToFewestLines(titleSize),
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
                        labels = labels,
                        capToNormalFont = true,
                        modifier = Modifier.padding(start = TopBarSizes.gap),
                    )
                }
            }
        }
    }
}

/** The labels of the record: [record] until it is beaten, then the sticker [newRecord]. */
private data class RecordLabels(val record: String, val newRecord: String)

/**
 * The rows of the top bar [width] pixels wide ([topBarRows]): the room beside the pause button against the widths the
 * title, of [titleSize], and the record need at the smallest size they may shrink to ([shrinkFloor]): the longest word
 * of the title, and the widest of the record label, of the longest word of the sticker that replaces it once the record
 * is beaten (it takes two lines where one does not fit), and of its value. The sticker counts from the start: the bar
 * keeps its rows when the record is beaten.
 */
@Composable
private fun rememberTopBarRows(
    width: Int,
    title: String,
    titleSize: TextUnit,
    labels: RecordLabels,
    record: String,
): TopBarRows {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = BloxTheme.typography
    return remember(width, title, titleSize, labels, record, density, typography, measurer) {
        fun widthAtSmallestSize(
            text: String,
            style: TextStyle,
        ) = measurer.measure(
            text = AnnotatedString(text),
            style = style.copy(fontSize = density.shrinkFloor(style.fontSize)),
            maxLines = 1,
            softWrap = false,
        ).size.width.toFloat()

        val titleStyle = typography.gameTitle.copy(fontSize = titleSize)
        val titleWidth = title.split(' ').maxOf { widthAtSmallestSize(it, titleStyle) }
        val labelStyle = typography.label.copy(fontSize = PanelSizes.label)
        val stickerStyle = typography.pointsPop.copy(fontSize = PanelSizes.label)
        val valueStyle = typography.value.copy(fontSize = PanelSizes.value)
        val stickerPadding = with(density) { (TopBarSizes.stickerPaddingX * 2).toPx() }
        val recordWidth = maxOf(
            widthAtSmallestSize(labels.record.uppercase(), labelStyle),
            labels.newRecord.uppercase().split(" ").maxOf { widthAtSmallestSize(it, stickerStyle) } + stickerPadding,
            widthAtSmallestSize(record, valueStyle),
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
        style = BloxTheme.typography.gameTitle,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        autoSize = autoSize,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * The [record], its label above its value from [texts], aligned on the right; TalkBack reads it as one, with the real
 * value. Its label is RECORD, or once beaten the sticker NOUVEAU RECORD, both of [labels]: the two are laid out one
 * over the other, the one not shown invisible, so that the record keeps its size when it is beaten. A record whose
 * value is unknown keeps the place of its value, hidden. With [capToNormalFont], it is never larger than with the
 * normal font.
 */
@Composable
private fun Record(
    record: ShownRecord,
    texts: TopBarTexts,
    labels: RecordLabels,
    modifier: Modifier = Modifier,
    capToNormalFont: Boolean = false,
) {
    val label = if (record.beaten) labels.newRecord else labels.record
    val description = if (record.value == null) {
        label
    } else {
        valueDescription(label, texts.spokenRecord)
    }
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            PanelLabel(
                text = labels.record,
                capToNormalFont = capToNormalFont,
                modifier = Modifier.alpha(if (record.beaten) 0f else 1f),
            )
            NewRecordSticker(
                text = labels.newRecord,
                capToNormalFont = capToNormalFont,
                modifier = Modifier.alpha(if (record.beaten) 1f else 0f),
            )
        }
        ValueText(
            text = texts.record,
            modifier = Modifier.alpha(if (record.value == null) 0f else 1f),
            capToNormalFont = capToNormalFont,
        )
    }
}

/**
 * The sticker NOUVEAU RECORD: [text] in capitals, `accent-ink` on `accent`, in the font of the stickers, in a pill
 * that leans; on as few lines as it can, shrinking as the label it replaces, on two lines where one does not fit,
 * as with an enlarged font. With [capToNormalFont], it is never larger than with the normal font.
 */
@Composable
private fun NewRecordSticker(
    text: String,
    capToNormalFont: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val largest = if (capToNormalFont) LocalDensity.current.notEnlarged(PanelSizes.label) else PanelSizes.label
    val tilt = if (BloxTheme.kind == BloxThemeKind.ORIGINAL) 0f else STICKER_TILT
    Text(
        text = text.uppercase(),
        color = palette.accentInk,
        style = BloxTheme.typography.pointsPop,
        maxLines = 2,
        textAlign = TextAlign.End,
        autoSize = shrinkToFewestLines(largest),
        modifier = modifier
            .graphicsLayer { rotationZ = tilt }
            .background(palette.accent, BloxTheme.shapes.pill)
            .padding(horizontal = TopBarSizes.stickerPaddingX, vertical = TopBarSizes.stickerPaddingY),
    )
}

/** The pause button: a disc of the family Icône, its glyph drawn ([pauseBars]), not a Unicode character. */
@Composable
private fun PauseButton(
    onPause: () -> Unit,
    enabled: Boolean,
) {
    GlyphButton(
        description = stringResource(R.string.feature_game_top_pause),
        onClick = onPause,
        enabled = enabled,
        // Read by the measured journey of `:benchmark` (`By.res`), whatever the language.
        modifier = Modifier.testTag("game_pause"),
    ) { ink ->
        pauseBars(size).forEach { bar -> drawRect(ink, topLeft = bar.topLeft, size = bar.size) }
    }
}

@Composable
private fun TopBarPreview(
    kind: BloxThemeKind,
    record: ShownRecord = ShownRecord(value = 123_450, beaten = false),
) {
    BloxTheme(kind = kind) {
        GameTopBar(
            record = record,
            onPause = {},
            modifier = Modifier
                .width(360.dp)
                .background(BloxTheme.palette.page)
                .padding(TopBarSizes.gap),
        )
    }
}

/** A record beaten: the score of 123 460 is the new record. */
private val newRecord = ShownRecord(value = 123_460, beaten = true)

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

@Preview(widthDp = 360)
@Composable
private fun GameTopBarNewRecordOriginalPreview() {
    TopBarPreview(BloxThemeKind.ORIGINAL, newRecord)
}

@Preview(widthDp = 360)
@Composable
private fun GameTopBarNewRecordColorfulPreview() {
    TopBarPreview(BloxThemeKind.COLORFUL, newRecord)
}

@Preview(widthDp = 360, fontScale = 2f)
@Composable
internal fun GameTopBarNewRecordLargeFontPreview() {
    TopBarPreview(BloxThemeKind.COLORFUL, newRecord)
}
