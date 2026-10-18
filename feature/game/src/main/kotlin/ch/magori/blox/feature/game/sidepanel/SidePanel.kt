package ch.magori.blox.feature.game.sidepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.LabelledPanel
import ch.magori.blox.feature.game.ui.ValueText

/** The sizes of the side column, in one place. */
private object SidePanelSizes {
    /** Between two panels. */
    val gap = 8.dp

    /** The preview fits the widest piece: 2 cells high. */
    const val PREVIEW_ROWS = 2
}

/**
 * The side column of the game screen: score, level, lines and next piece, each in a panel. No
 * logic here: the texts come from [scoreText], [levelText] and [linesText], the preview from
 * [NextPieceCanvas], with cells of [cellSize] pixels, those of the board. [next] is null when the
 * next piece is hidden (pause, option).
 */
@Composable
internal fun SidePanel(
    score: Points,
    level: Level,
    lines: Lines,
    next: PieceType?,
    rotationSystem: RotationSystem,
    cellSize: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SidePanelSizes.gap)) {
        ScorePanels(score = score, level = level, lines = lines)
        NextPiecePanel(next = next, rotationSystem = rotationSystem, cellSize = cellSize)
    }
}

/** Score, level and lines, each in a panel, one under the other. */
@Composable
internal fun ScorePanels(
    score: Points,
    level: Level,
    lines: Lines,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SidePanelSizes.gap)) {
        ValuePanel(
            label = stringResource(R.string.feature_game_score),
            value = scoreText(score),
            rawValue = score.value,
        )
        ValuePanel(
            label = stringResource(R.string.feature_game_level),
            value = levelText(level),
            rawValue = level.value,
        )
        ValuePanel(
            label = stringResource(R.string.feature_game_lines),
            value = linesText(lines),
            rawValue = lines.value,
        )
    }
}

/** The next piece in its panel, with cells of [cellSize] pixels, those of the board. */
@Composable
internal fun NextPiecePanel(
    next: PieceType?,
    rotationSystem: RotationSystem,
    cellSize: Float,
    modifier: Modifier = Modifier,
) {
    val nextLabel = stringResource(R.string.feature_game_next)
    val nextName = next?.let { pieceName(it) } ?: stringResource(R.string.feature_game_next_none)
    val previewHeight = with(LocalDensity.current) { (cellSize * SidePanelSizes.PREVIEW_ROWS).toDp() }
    LabelledPanel(
        label = nextLabel,
        description = stringResource(R.string.feature_game_value_description, nextLabel, nextName),
        modifier = modifier,
    ) {
        NextPieceCanvas(
            type = next,
            rotationSystem = rotationSystem,
            cellSize = cellSize,
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight),
        )
    }
}

@Composable
private fun ValuePanel(
    label: String,
    value: String,
    rawValue: Int,
) {
    LabelledPanel(
        label = label,
        description = stringResource(R.string.feature_game_value_description, label, rawValue.toString()),
    ) {
        ValueText(text = value)
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
            score = Points(score),
            level = Level(level),
            lines = Lines(lines),
            next = next,
            rotationSystem = ClassicRotationSystem,
            cellSize = with(LocalDensity.current) { 18.dp.roundToPx().toFloat() },
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
