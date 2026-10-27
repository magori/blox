package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.BoardRowsCanvas
import ch.magori.blox.core.board.logo.BloxLogo
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Points

/**
 * The hero of the home in Colorful (V27, P5.77): the logo BloX v2, read « BloX » as the heading of the screen, then the
 * preview board, where a T falls at the gravity of the [level] chosen and clears two lines, with their real points. The
 * preview is a decoration, drawn from [motion] without recomposing.
 */
@Composable
internal fun HomeHero(
    level: Level,
    motion: HomeMotion,
    still: Boolean,
) {
    val title = stringResource(R.string.feature_home_title)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BloxLogo(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = title
                heading()
            },
        )
        PreviewBoard(
            level = level,
            replay = if (still) {
                remember(level) { { PreviewReplay.still(level) } }
            } else {
                remember(motion) { { motion.replay } }
            },
        )
    }
}

/**
 * The preview board: the bottom rows of the board of [replay] in a frame of `board`, and beside it SCORE and its
 * points, drawn without recomposing. Decorative: TalkBack reads nothing of it.
 */
@Composable
private fun PreviewBoard(
    level: Level,
    replay: () -> PreviewReplay,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    Row(
        modifier = Modifier.testTag(HOME_PREVIEW_TAG).clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
        verticalAlignment = Alignment.Bottom,
    ) {
        val board = remember(replay) { { replay().board } }
        val piece = remember(replay) { { replay().piece } }
        BoardRowsCanvas(
            board = board,
            piece = piece,
            rows = PreviewReplay.PREVIEW_ROWS,
            modifier = Modifier
                .background(palette.boardBackground, shape)
                .border(BloxBorders.frame, palette.panelBorder, shape)
                .padding(PREVIEW_PADDING)
                .size(PREVIEW_CELL * PREVIEW_COLUMNS, PREVIEW_CELL * PreviewReplay.PREVIEW_ROWS),
        )
        Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
            Text(
                text = stringResource(R.string.feature_home_preview_score).uppercase(),
                color = palette.label,
                style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel),
            )
            PreviewPoints(level, replay)
        }
    }
}

/**
 * The points of [replay], 0 until its lines clear, then theirs at [level], on 6 digits as in a game: both laid out
 * once per level, the one to show chosen while drawing.
 */
@Composable
private fun PreviewPoints(
    level: Level,
    replay: () -> PreviewReplay,
) {
    val measurer = rememberTextMeasurer()
    val style = BloxTheme.typography.statValue.copy(color = BloxTheme.palette.ink)
    val none = remember(measurer, style) { measurer.measure(scoreText(0), style) }
    val cleared = remember(measurer, style, level) {
        measurer.measure(scoreText(PreviewReplay.still(level).points.value), style)
    }
    val size = with(LocalDensity.current) {
        DpSize(
            maxOf(none.size.width, cleared.size.width).toDp(),
            maxOf(none.size.height, cleared.size.height).toDp(),
        )
    }
    Spacer(
        modifier = Modifier
            .size(size)
            .drawWithCache {
                onDrawBehind { drawText(if (replay().points == Points(0)) none else cleared) }
            },
    )
}

/** The side of a cell of the preview: under 16 dp, its bubbles have small corners and no gloss. */
private val PREVIEW_CELL = 14.dp
private const val PREVIEW_COLUMNS = 10
private val PREVIEW_PADDING = 4.dp
