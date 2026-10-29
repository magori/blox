package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.locksmith.PadlockTier
import ch.magori.blox.feature.locksmith.PadlocksItem.Cell
import ch.magori.blox.feature.locksmith.PadlocksItem.TierHeader

/** What the player does on the screen « Serrures », handed to `app` by the entry. */
@Immutable
internal class PadlocksActions(val onBack: () -> Unit, val onOpen: (level: Int) -> Unit) {
    companion object {
        /** Actions that do nothing, for the previews. */
        val None = PadlocksActions(onBack = {}, onOpen = {})
    }
}

/**
 * The screen « Serrures » (P7.35), drawn from [state] alone, always in the Colorful theme, day or night as the system
 * says, as the Locksmith (ADR 0025). From the top: the bar (back, the title), the summary, « n ouvertes · m parfaites »,
 * or, before the first opening, « Ouvre la serrure 1 pour commencer. », then Reprendre, which opens the frontier. Below,
 * the grid of the padlocks, a header per tier, opened scrolled on the frontier: the grid ends on its row. The padlock
 * the player comes from, [current], if known, is told « en cours ». A tile open or the frontier asks to open its
 * padlock, by [PadlocksActions.onOpen].
 */
@Composable
internal fun PadlocksScreen(
    state: PadlocksUiState,
    current: Int?,
    actions: PadlocksActions,
    modifier: Modifier = Modifier,
) {
    ColorfulTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BloxTheme.palette.page)
                .safeDrawingPadding()
                .padding(horizontal = SIDE_MARGIN),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
        ) {
            PadlocksBar(actions.onBack)
            PadlocksGrid(state, current, actions.onOpen, Modifier.weight(1f))
        }
    }
}

/** Back, then the title of the screen, in capitals as the titles of the other screens; TalkBack reads it in words. */
@Composable
private fun PadlocksBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BloxSpaces.space3),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_locksmith_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        val title = stringResource(R.string.feature_locksmith_padlocks_title)
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = title
                    heading()
                },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.gameTitle,
            autoSize = shrinkToWholeWords(BloxTheme.typography.gameTitle.fontSize),
        )
    }
}

/** « n ouvertes · m parfaites », a heading; before the first opening, the invitation to open padlock 1. */
@Composable
private fun Summary(state: PadlocksUiState) {
    if (state.isEmpty) {
        Text(
            text = stringResource(R.string.feature_locksmith_padlocks_empty),
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.section,
        )
    } else {
        Text(
            text = stringResource(
                R.string.feature_locksmith_padlocks_summary,
                pluralStringResource(R.plurals.feature_locksmith_padlocks_open, state.open, state.open),
                pluralStringResource(R.plurals.feature_locksmith_padlocks_perfect, state.perfect, state.perfect),
            ),
            modifier = Modifier.semantics { heading() },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.section,
        )
    }
}

/**
 * The summary and Reprendre, then the grid of the padlocks ([padlocksGridOf]), scrolling together, so that a large font
 * on a low screen still leaves room for the tiles; a collection for TalkBack: tiles of about [TILE], fewer columns in a
 * large font ([padlockColumns]), a header across the grid per tier. It opens on the frontier, and keeps where it was
 * scrolled to when the screen comes back.
 */
@Composable
private fun PadlocksGrid(
    state: PadlocksUiState,
    current: Int?,
    onOpen: (level: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = padlockColumns(maxWidth, LocalDensity.current.fontScale)
        val items = remember(state, current, columns) { padlocksGridOf(state.openings, current, columns) }
        // The summary, and Reprendre once a padlock is open, come before the items of the grid.
        val above = if (state.isEmpty) 1 else 2
        val frontier = remember(items) { items.indexOfFirst { it is Cell && it.state == CellState.FRONTIER } }
        val grid = rememberLazyGridState(initialFirstVisibleItemIndex = above + frontier)
        // The height may change after the first frame, as the insets of the system come, and cut the row of the
        // frontier: the grid is scrolled on it again, as long as the player has not dragged it.
        val dragged by grid.interactionSource.collectIsDraggedAsState()
        var draggedOnce by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(dragged) { if (dragged) draggedOnce = true }
        LaunchedEffect(maxHeight) { if (!draggedOnce) grid.scrollToItem(above + frontier) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag(PADLOCKS_GRID_TAG),
            state = grid,
            contentPadding = PaddingValues(bottom = BloxSpaces.space4),
            verticalArrangement = Arrangement.spacedBy(GAP),
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            item(key = SUMMARY_KEY, span = { GridItemSpan(maxLineSpan) }) { Summary(state) }
            if (!state.isEmpty) {
                item(key = RESUME_KEY, span = { GridItemSpan(maxLineSpan) }) {
                    PrimaryButton(
                        text = stringResource(R.string.feature_locksmith_padlocks_resume, state.frontier),
                        icon = BloxIcon.PLAY,
                        onClick = { onOpen(state.frontier) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = BloxSpaces.space2),
                    )
                }
            }
            items(
                items = items,
                key = { item ->
                    when (item) {
                        is TierHeader -> "tier_${item.tier.name}"
                        is Cell -> item.level
                    }
                },
                span = { item -> GridItemSpan(if (item is TierHeader) maxLineSpan else 1) },
                contentType = { item -> item::class },
            ) { item ->
                when (item) {
                    is TierHeader -> TierHeading(item.tier)
                    is Cell -> PadlockTile(item, onOpen)
                }
            }
        }
    }
}

/** The header of [tier]: its name, then the pieces of its padlocks, « Apprenti · 3 à 5 pièces », a heading. */
@Composable
private fun TierHeading(tier: PadlockTier) {
    val pieces = tier.pieces
    Text(
        text = stringResource(
            R.string.feature_locksmith_padlocks_tier,
            stringResource(tierNameOf(tier)),
            stringResource(piecesTextOf(pieces), pieces.first, pieces.last),
        ),
        modifier = Modifier
            .padding(top = BloxSpaces.space2)
            .semantics { heading() },
        color = BloxTheme.palette.label,
        style = BloxTheme.typography.statLabel,
    )
}

private fun tierNameOf(tier: PadlockTier): Int = when (tier) {
    PadlockTier.APPRENTICE -> R.string.feature_locksmith_padlocks_tier_apprentice
    PadlockTier.JOURNEYMAN -> R.string.feature_locksmith_padlocks_tier_journeyman
    PadlockTier.CRAFTSMAN -> R.string.feature_locksmith_padlocks_tier_craftsman
    PadlockTier.MASTER -> R.string.feature_locksmith_padlocks_tier_master
}

/**
 * The tile of [cell]: its number and a pictogram (a padlock, open once opened; a star once perfect), then its words
 * ([wordsOf]), which tell its state; its colours only double them. Read by TalkBack in one go, « Serrure 5, ouverte,
 * parfaite, meilleure : 4 coups, 0 refus, 0 indice ». An open padlock and the frontier ask to be opened when touched;
 * a locked one is neither touched nor offered.
 */
@Composable
private fun PadlockTile(
    cell: Cell,
    onOpen: (level: Int) -> Unit,
) {
    val look = tileLookOf(cell, BloxTheme.palette)
    val shape = BloxTheme.shapes.md
    val reading = tileReading(cell)
    val touchable = cell.state != CellState.LOCKED
    val openLabel = stringResource(R.string.feature_locksmith_padlocks_open_action)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TILE)
            .clip(shape)
            .background(look.face, shape)
            .border(look.edgeWidth, look.edge, shape)
            .then(
                if (touchable) {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { onOpen(cell.level) }
                } else {
                    Modifier
                },
            )
            // Read in one go: its words and pictograms cleared, the tag kept for the tests.
            .clearAndSetSemantics {
                contentDescription = reading
                testTag = padlockTileTag(cell.level)
                if (touchable) {
                    role = Role.Button
                    onClick(label = openLabel) {
                        onOpen(cell.level)
                        true
                    }
                }
            }
            .padding(BloxSpaces.space2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = cell.level.toString(), color = look.ink, style = BloxTheme.typography.statValue)
            val open = cell.state == CellState.OPEN || cell.state == CellState.PERFECT
            Canvas(modifier = Modifier.size(PICTOGRAM)) { drawPadlock(look.ink, look.face, open) }
            if (cell.state == CellState.PERFECT) {
                BloxIcon(BloxIcon.STAR, size = PICTOGRAM, tint = look.ink)
            }
        }
        wordsOf(cell).forEach { word ->
            Text(
                text = stringResource(word),
                color = look.ink,
                style = BloxTheme.typography.statLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** What TalkBack reads of [cell]: « Serrure n », its state ([readOf]), then its best opening, if any. */
@Composable
@ReadOnlyComposable
private fun tileReading(cell: Cell): String {
    val parts = buildList {
        add(stringResource(R.string.feature_locksmith_padlocks_cell, cell.level))
        readOf(cell).forEach { add(stringResource(it)) }
        cell.best?.let { best ->
            val hints = pluralStringResource(R.plurals.feature_locksmith_open_best_hints, best.hints, best.hints)
            add(stringResource(R.string.feature_locksmith_padlocks_best, best.moves, best.refusals, hints))
        }
    }
    return parts.joinToString(", ")
}

/** The colours of a tile: its [face], its [ink], its [edge] of [edgeWidth]. */
private data class TileLook(val face: Color, val ink: Color, val edge: Color, val edgeWidth: Dp)

/**
 * The look of [cell], which doubles its words: an open padlock on a panel, a perfect one in the accent, the frontier as
 * a button, its edge thicker; a locked one on the page, its ink dimmer, its edge faint, as nothing to touch. The
 * padlock the player comes from has the thickest edge, in ink.
 */
private fun tileLookOf(
    cell: Cell,
    palette: BloxPalette,
): TileLook {
    val look = when (cell.state) {
        CellState.OPEN -> TileLook(palette.panel, palette.ink, palette.buttonOutline, HAIRLINE)
        CellState.PERFECT -> TileLook(palette.accent, palette.accentInk, palette.buttonOutline, HAIRLINE)
        CellState.FRONTIER -> TileLook(palette.button, palette.buttonInk, palette.buttonOutline, BloxBorders.frame)
        CellState.LOCKED -> TileLook(palette.page, palette.label, palette.panelBorder, HAIRLINE)
    }
    return if (cell.current) look.copy(edge = palette.ink, edgeWidth = BloxBorders.focus) else look
}

/** The edge of a tile at rest. */
private val HAIRLINE = 1.dp

/** The padlock and the star of a tile, beside its number. */
private val PICTOGRAM = 16.dp

/** The keys of the summary and of Reprendre, above the tiles in the grid, apart from the levels that key the tiles. */
private const val SUMMARY_KEY = "summary"
private const val RESUME_KEY = "resume"

/** The tag of the grid of the padlocks. */
internal const val PADLOCKS_GRID_TAG = "padlocks_grid"

/** The tag of the tile of padlock [level]. */
internal fun padlockTileTag(level: Int): String = "padlocks_tile_$level"
