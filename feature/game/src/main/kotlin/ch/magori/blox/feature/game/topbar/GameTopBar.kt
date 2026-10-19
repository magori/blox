package ch.magori.blox.feature.game.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.PanelLabel
import ch.magori.blox.feature.game.ui.ValueText
import ch.magori.blox.feature.game.ui.shrinkToFit

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
 * logic here: the record texts come from [topBarTexts], the glyph from [pauseBars]. A `null` [record]
 * is not known yet: its value is hidden, in a place of the same size, so that nothing moves when it
 * comes.
 */
@Composable
internal fun GameTopBar(
    record: Int?,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val texts = topBarTexts(record ?: 0)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TopBarSizes.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PauseButton(onPause = onPause)
        // On two lines, shrinking if needed: with an enlarged font, back to its size with the normal font, where
        // CLASSIQUE fits whole, rather than cut.
        Text(
            text = stringResource(R.string.feature_game_top_title).uppercase(),
            color = palette.ink,
            style = BloxTheme.typography.title,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            autoSize = shrinkToFit(TopBarSizes.title),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        val recordLabel = stringResource(R.string.feature_game_top_record)
        val description = if (record == null) {
            recordLabel
        } else {
            stringResource(R.string.feature_game_value_description, recordLabel, texts.spokenRecord)
        }
        Column(
            horizontalAlignment = Alignment.End,
            // At most as wide as the title: with an enlarged font, the record shrinks rather than the title.
            modifier = Modifier
                .weight(1f, fill = false)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            PanelLabel(text = recordLabel)
            ValueText(text = texts.record, modifier = Modifier.alpha(if (record == null) 0f else 1f))
        }
    }
}

/** The pause button: a drawn glyph, not a Unicode character, on a touch target of at least 48 dp. */
@Composable
private fun PauseButton(onPause: () -> Unit) {
    val palette = BloxTheme.palette
    val description = stringResource(R.string.feature_game_top_pause)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // Read by the measured journey of `:benchmark` (`By.res`), whatever the language.
            .testTag("game_pause")
            .sizeIn(minWidth = BloxSizes.minTouchTarget, minHeight = BloxSizes.minTouchTarget)
            .clip(BloxTheme.shapes.button)
            .background(palette.button)
            .clickable(role = Role.Button, onClick = onPause)
            .semantics { contentDescription = description },
    ) {
        val ink = palette.buttonInk
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
