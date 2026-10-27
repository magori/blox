package ch.magori.blox.feature.game.sidepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.board.next.NextPieceCanvas
import ch.magori.blox.core.board.next.NextPieceCells
import ch.magori.blox.core.board.next.nextPieceCells
import ch.magori.blox.core.board.next.nextPieceName
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.LabelledPanel
import ch.magori.blox.feature.game.ui.ValueText
import ch.magori.blox.feature.game.ui.valueDescription

/** The sizes of the side column, in one place. */
internal object SidePanelSizes {
    /** Between two panels. */
    val gap = 8.dp

    /** The preview fits every piece in its spawn orientation: 4 cells wide, 2 high. */
    const val PREVIEW_COLUMNS = 4
    const val PREVIEW_ROWS = 2
}

/**
 * The side column of the game screen (views V3, V28): the next piece at the top, near where it appears, then score,
 * lines and level, each in a panel. No logic here: the texts come from [scoreText], [levelText] and [linesText], the
 * fill of the record bar under the score, [recordFill], from [recordBarFill], the preview from [NextPieceCanvas], with
 * cells of [cellSize] pixels, those of the board, and [previewModifier] (the curtain of the pause). [next] is null
 * when the next piece is hidden (pause, option).
 */
@Composable
internal fun SidePanel(
    score: Points,
    level: Level,
    lines: Lines,
    next: NextPieceCells?,
    recordFill: Float?,
    cellSize: Float,
    modifier: Modifier = Modifier,
    previewModifier: Modifier = Modifier,
    shownScore: (() -> Points)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SidePanelSizes.gap)) {
        NextPiecePanel(next = next, cellSize = cellSize, previewModifier = previewModifier)
        ScorePanels(score = score, level = level, lines = lines, recordFill = recordFill, shownScore = shownScore)
    }
}

/** Score, its record bar filled by [recordFill], then lines and level, each in a panel, one under the other. */
@Composable
internal fun ScorePanels(
    score: Points,
    level: Level,
    lines: Lines,
    recordFill: Float?,
    modifier: Modifier = Modifier,
    shownScore: (() -> Points)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SidePanelSizes.gap)) {
        ScorePanel(score, recordFill, shownScore = shownScore)
        LinesPanel(lines)
        LevelPanel(level)
    }
}

/**
 * The side column laid across, above the board of a very narrow window: next piece and score, then lines and level,
 * in the order of the side column; the next piece with cells of [cellSize] pixels, those of the board, its preview
 * with [previewModifier], the score with its record bar filled by [recordFill]. The two panels of a row are as wide
 * and as high as each other. [next] is null when the next piece is hidden.
 */
@Composable
internal fun ScoreGrid(
    score: Points,
    level: Level,
    lines: Lines,
    next: NextPieceCells?,
    recordFill: Float?,
    cellSize: Float,
    modifier: Modifier = Modifier,
    previewModifier: Modifier = Modifier,
    shownScore: (() -> Points)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SidePanelSizes.gap)) {
        PanelRow {
            NextPiecePanel(
                next = next,
                cellSize = cellSize,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                previewModifier = previewModifier,
            )
            ScorePanel(score, recordFill, Modifier.weight(1f).fillMaxHeight(), shownScore)
        }
        PanelRow {
            LinesPanel(lines, Modifier.weight(1f).fillMaxHeight())
            LevelPanel(level, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Two panels side by side, [SidePanelSizes.gap] apart, as high as the higher. */
@Composable
private fun PanelRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(SidePanelSizes.gap),
        content = content,
    )
}

/**
 * The score in its panel, the record bar under it filled by [recordFill] ([recordBar]); without a record, none. The
 * value shown is the one [shownScore] gives, the score itself without it: a score that counts up recomposes only its
 * text ([ScoreValue], ADR 0024). TalkBack reads the score itself.
 */
@Composable
private fun ScorePanel(
    score: Points,
    recordFill: Float?,
    modifier: Modifier = Modifier,
    shownScore: (() -> Points)? = null,
) {
    val bar = if (recordFill == null) Modifier else Modifier.recordBar(recordFill)
    ValuePanel(stringResource(R.string.feature_game_score), score.value, modifier.then(bar)) {
        if (shownScore == null) ValueText(text = scoreText(score)) else ScoreValue(shownScore)
    }
}

/** The score [shown] gives, followed on its own: it recomposes alone as the score counts. */
@Composable
private fun ScoreValue(shown: () -> Points) {
    val score by remember(shown) { derivedStateOf(shown) }
    ValueText(text = scoreText(score))
}

@Composable
private fun LevelPanel(
    level: Level,
    modifier: Modifier = Modifier,
) {
    ValuePanel(stringResource(R.string.feature_game_level), levelText(level), level.value, modifier)
}

@Composable
private fun LinesPanel(
    lines: Lines,
    modifier: Modifier = Modifier,
) {
    ValuePanel(stringResource(R.string.feature_game_lines), linesText(lines), lines.value, modifier)
}

/**
 * The next piece in its panel, with cells of [cellSize] pixels, those of the board; its preview takes
 * [previewModifier] (the curtain of the pause).
 */
@Composable
internal fun NextPiecePanel(
    next: NextPieceCells?,
    cellSize: Float,
    modifier: Modifier = Modifier,
    previewModifier: Modifier = Modifier,
) {
    val nextLabel = stringResource(R.string.feature_game_next)
    val nextName = nextPieceName(next)
    val previewHeight = with(LocalDensity.current) { (cellSize * SidePanelSizes.PREVIEW_ROWS).toDp() }
    LabelledPanel(
        label = nextLabel,
        description = valueDescription(nextLabel, nextName),
        modifier = modifier,
    ) {
        NextPieceCanvas(
            next = next,
            cellSize = cellSize,
            modifier = previewModifier
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
    modifier: Modifier = Modifier,
) {
    ValuePanel(label, rawValue, modifier) { ValueText(text = value) }
}

/** A panel of a value, its [label] above its [content]; TalkBack reads the label and [rawValue]. */
@Composable
private fun ValuePanel(
    label: String,
    rawValue: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LabelledPanel(
        label = label,
        description = valueDescription(label, rawValue.toString()),
        modifier = modifier,
    ) {
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
    record: Int? = 98_760,
) {
    BloxTheme(kind = kind) {
        SidePanel(
            score = Points(score),
            level = Level(level),
            lines = Lines(lines),
            next = next?.let { nextPieceCells(it, ClassicRotationSystem) },
            recordFill = recordBarFill(Points(score), record),
            cellSize = with(LocalDensity.current) { 18.dp.roundToPx().toFloat() },
            modifier = Modifier
                .background(BloxTheme.palette.page)
                .padding(8.dp)
                .width(120.dp),
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview
@Composable
private fun SidePanelOriginalPreview() {
    SidePanelPreview(BloxThemeKind.ORIGINAL, score = 12_340, level = 5, lines = 52, next = PieceType.T)
}

@Preview
@Composable
internal fun SidePanelColorfulPreview() {
    SidePanelPreview(BloxThemeKind.COLORFUL, score = 1_234_567, level = 19, lines = 190, next = PieceType.I)
}

@Preview(fontScale = 2f)
@Composable
internal fun SidePanelLargeFontPreview() {
    SidePanelPreview(BloxThemeKind.ORIGINAL, score = 999_999, level = 29, lines = 1000, next = PieceType.L)
}

@Preview
@Composable
internal fun SidePanelNoNextPiecePreview() {
    SidePanelPreview(BloxThemeKind.COLORFUL, score = 0, level = 0, lines = 0, next = null, record = null)
}
