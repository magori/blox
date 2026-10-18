package ch.magori.blox.feature.game.sidepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.R

/** The sizes of the side column, in one place. */
private object SidePanelSizes {
    /** Between two panels, and inside a panel around its content. */
    val gap = 8.dp

    /** Between the label and the value of a panel. */
    val labelGap = 4.dp
    val border = 2.dp
    val label = 11.sp

    /** The largest size of a value; it shrinks to stay on one line. */
    val value = 18.sp

    /** The preview fits the widest piece: 4 cells wide, 2 cells high. */
    const val PREVIEW_ASPECT_RATIO = 4f / 2f
}

/**
 * The side column of the game screen: score, level, lines and next piece, each in a panel. No
 * logic here: the texts come from [scoreText], [levelText] and [linesText], the preview from
 * [NextPieceCanvas]. [next] is null when the next piece is hidden (pause, option). It takes plain
 * values until the game screen has its UI state.
 */
@Composable
internal fun SidePanel(
    score: Int,
    level: Int,
    lines: Int,
    next: PieceType?,
    rotationSystem: RotationSystem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SidePanelSizes.gap)) {
        ValuePanel(label = stringResource(R.string.feature_game_score), value = scoreText(score), rawValue = score)
        ValuePanel(label = stringResource(R.string.feature_game_level), value = levelText(level), rawValue = level)
        ValuePanel(label = stringResource(R.string.feature_game_lines), value = linesText(lines), rawValue = lines)
        val nextLabel = stringResource(R.string.feature_game_next)
        val nextName = next?.let { pieceName(it) } ?: stringResource(R.string.feature_game_next_none)
        Panel(
            label = nextLabel,
            description = stringResource(R.string.feature_game_value_description, nextLabel, nextName),
        ) {
            NextPieceCanvas(
                type = next,
                rotationSystem = rotationSystem,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SidePanelSizes.PREVIEW_ASPECT_RATIO),
            )
        }
    }
}

@Composable
private fun ValuePanel(
    label: String,
    value: String,
    rawValue: Int,
) {
    Panel(
        label = label,
        description = stringResource(R.string.feature_game_value_description, label, rawValue.toString()),
    ) {
        Text(
            text = value,
            color = BloxTheme.palette.ink,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(maxFontSize = SidePanelSizes.value),
        )
    }
}

/** A panel: a [label] above its [content]; TalkBack reads it as one [description]. */
@Composable
private fun Panel(
    label: String,
    description: String,
    content: @Composable () -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
            .background(palette.panel, shape)
            .border(SidePanelSizes.border, palette.panelBorder, shape)
            .padding(SidePanelSizes.gap),
        verticalArrangement = Arrangement.spacedBy(SidePanelSizes.labelGap),
    ) {
        Text(text = label.uppercase(), color = palette.label, fontSize = SidePanelSizes.label, maxLines = 1)
        content()
    }
}

@Composable
private fun SidePanelPreview(
    kind: BloxThemeKind,
    score: Int,
    level: Int,
    lines: Int,
    next: PieceType?,
) {
    BloxTheme(kind = kind) {
        SidePanel(
            score = score,
            level = level,
            lines = lines,
            next = next,
            rotationSystem = ClassicRotationSystem,
            modifier = Modifier
                .background(BloxTheme.palette.page)
                .padding(8.dp)
                .width(120.dp),
        )
    }
}

@Preview
@Composable
private fun SidePanelOriginalPreview() {
    SidePanelPreview(BloxThemeKind.ORIGINAL, score = 12_340, level = 5, lines = 52, next = PieceType.T)
}

@Preview
@Composable
private fun SidePanelColorfulPreview() {
    SidePanelPreview(BloxThemeKind.COLORFUL, score = 1_234_567, level = 19, lines = 190, next = PieceType.I)
}

@Preview(fontScale = 2f)
@Composable
private fun SidePanelLargeFontPreview() {
    SidePanelPreview(BloxThemeKind.ORIGINAL, score = 999_999, level = 29, lines = 1000, next = PieceType.L)
}

@Preview
@Composable
private fun SidePanelNoNextPiecePreview() {
    SidePanelPreview(BloxThemeKind.COLORFUL, score = 0, level = 0, lines = 0, next = null)
}
