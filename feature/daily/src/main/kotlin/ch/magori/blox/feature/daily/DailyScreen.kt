package ch.magori.blox.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.board.next.NextPieceCanvas
import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.PlayButton
import ch.magori.blox.core.designsystem.component.ScoreChip
import ch.magori.blox.core.designsystem.component.SettingsButton
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import java.time.LocalDate

/** The tag of « Copier mon score », read by the tests. */
internal const val DAILY_COPY_TAG = "daily_copy"

/**
 * The tag of Jouer, the same in every language, read by the journey of `:benchmark`, which copies it as it is: it does
 * not see this module.
 */
internal const val DAILY_PLAY_TAG = "daily_play"

/** What the player does on the entry of the Graine du jour, handed to `app` by the entry. */
@Immutable
internal class DailyActions(
    val onChangeGame: () -> Unit,
    val onSettings: () -> Unit,
    val onPlay: (date: LocalDate) -> Unit,
    val onCopyScore: (date: LocalDate) -> Unit,
) {
    companion object {
        /** Actions that do nothing, for the previews. */
        val None = DailyActions({}, {}, {}, {})
    }
}

/**
 * The entry of the Graine du jour (V38), on the model of the home of the Classic (V27), in one column that scrolls on a
 * small phone: the bar (Changer de jeu, the best of the day, Paramètres), the title « Graine du 26 octobre », the
 * first pieces of the seed, the tiles of the results, Jouer XL and its reminder, « Copier mon score », and « Nouvelle
 * graine à minuit » at the bottom. Drawn in the Colorful theme only, day or night as the phone, the effects of the
 * application kept (ADR 0026). No logic: what each action means belongs to the view model and to `app`. Taps on what
 * leaves the entry count only while it is resumed: once a game is on its way, a second tap does nothing. Jouer hands
 * the date shown, so that the game draws the pieces shown, even once the phone passed midnight.
 */
@Composable
internal fun DailyScreen(
    uiState: DailyUiState,
    actions: DailyActions,
    modifier: Modifier = Modifier,
) {
    val changeGame = dropUnlessResumed { actions.onChangeGame() }
    val openSettings = dropUnlessResumed { actions.onSettings() }
    val play = dropUnlessResumed { actions.onPlay(uiState.date) }
    ColorfulTheme {
        val palette = BloxTheme.palette
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(palette.page)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(BloxSpaces.space4),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DailyBar(best = uiState.best, onChangeGame = changeGame, onSettings = openSettings)
            Text(
                text = stringResource(R.string.feature_daily_title, dayInLetters(uiState.date)),
                modifier = Modifier.semantics { heading() },
                color = palette.ink,
                style = BloxTheme.typography.screenTitle,
                textAlign = TextAlign.Center,
            )
            FirstPieces(uiState.firstPieces)
            ResultTiles(uiState)
            PlayButton(
                label = stringResource(R.string.feature_daily_play),
                reminder = stringResource(R.string.feature_daily_play_reminder, uiState.startLevel),
                description = stringResource(R.string.feature_daily_play_description, uiState.startLevel),
                onClick = play,
                modifier = Modifier.testTag(DAILY_PLAY_TAG),
            )
            CopyScore(enabled = uiState.canCopy, onClick = { actions.onCopyScore(uiState.date) })
            Text(
                text = stringResource(R.string.feature_daily_new_seed),
                color = palette.label,
                style = BloxTheme.typography.small,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The bar: the disc « Changer de jeu » at the start, the chip of the [best] of the day in the middle, the gear at the
 * end. Both discs are 48 dp, so that the chip stays in the middle.
 */
@Composable
private fun DailyBar(
    best: Int?,
    onChangeGame: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            icon = BloxIcon.CARTRIDGE,
            description = stringResource(R.string.feature_daily_change_game),
            onClick = onChangeGame,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            BestOfTheDayChip(best)
        }
        SettingsButton(onClick = onSettings)
    }
}

/**
 * The best of the day (puce « MEILLEUR DU JOUR »), the [ScoreChip] of the record of the home: the [best] capped on 6
 * digits, or « — » before any. TalkBack reads the real value, or that there is no try.
 */
@Composable
private fun BestOfTheDayChip(best: Int?) {
    ScoreChip(
        label = stringResource(R.string.feature_daily_best_of_the_day),
        value = resultText(best),
        description = if (best == null) {
            stringResource(R.string.feature_daily_best_of_the_day_none)
        } else {
            stringResource(R.string.feature_daily_best_of_the_day_description, best)
        },
    )
}

/**
 * « LES 7 PREMIÈRES »: the [pieces], numbered from 1, each drawn as the next piece of a game, in its spawn
 * orientation. Not touchable; TalkBack reads each « Pièce 1 : T ».
 */
@Composable
private fun FirstPieces(pieces: List<PieceType>) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.window
    val cellSize = with(LocalDensity.current) { PIECE_CELL.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(BloxSpaces.space3),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
    ) {
        Text(
            text = stringResource(R.string.feature_daily_first_pieces).uppercase(),
            color = palette.label,
            style = BloxTheme.typography.statLabel,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            pieces.forEachIndexed { index, type ->
                val rank = index + 1
                val description = stringResource(R.string.feature_daily_piece, rank, type.name)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
                ) {
                    SingleLineText(text = "$rank", style = BloxTheme.typography.statLabel, color = palette.label)
                    NextPieceCanvas(
                        next = nextPieceCells(type, ClassicRotationSystem),
                        cellSize = cellSize,
                        modifier = Modifier.fillMaxWidth().height(PIECE_HEIGHT),
                    )
                }
            }
        }
    }
}

/** The tiles Essais, Premier essai and Meilleur, side by side and as high as each other; « — » before any try. */
@Composable
private fun ResultTiles(uiState: DailyUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
    ) {
        val tile = Modifier.weight(1f).fillMaxHeight()
        val tries = uiState.tries.takeIf { it > 0 }
        ResultTile(stringResource(R.string.feature_daily_tries), tries, tries?.toString(), tile)
        ResultTile(stringResource(R.string.feature_daily_first_try), uiState.firstTry, null, tile)
        ResultTile(stringResource(R.string.feature_daily_best), uiState.best, null, tile)
    }
}

/**
 * A tile: its [label] above its [value], shown as [shown], else capped on 6 digits as a score, or « — » when null.
 * Read by TalkBack as one « label : value ».
 */
@Composable
private fun ResultTile(
    label: String,
    value: Int?,
    shown: String?,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    val description = if (value == null) {
        stringResource(R.string.feature_daily_tile_description_none, label)
    } else {
        stringResource(R.string.feature_daily_tile_description, label, value)
    }
    Column(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(BloxSpaces.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1, Alignment.CenterVertically),
    ) {
        Text(
            text = label.uppercase(),
            color = palette.label,
            style = BloxTheme.typography.statLabel,
            textAlign = TextAlign.Center,
            // Shrunk rather than cut in the middle of a word, in a large font (« PREMIER ESSAI » in a third of the line).
            autoSize = shrinkToWholeWords(BloxTheme.typography.statLabel.fontSize),
        )
        SingleLineText(text = shown ?: resultText(value), style = BloxTheme.typography.statValue, color = palette.ink)
    }
}

/**
 * « Copier mon score », a secondary button as wide as the column; disabled before the first try of the day, with
 * « Joue une partie pour copier ton score » under it.
 */
@Composable
private fun CopyScore(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
    ) {
        BubbleButton(
            family = ButtonFamily.SECONDARY,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().testTag(DAILY_COPY_TAG),
        ) {
            BloxIcon(BloxIcon.COPY)
            Text(
                text = stringResource(R.string.feature_daily_copy_score),
                modifier = Modifier.padding(start = BloxSpaces.space2),
            )
        }
        if (!enabled) {
            Text(
                text = stringResource(R.string.feature_daily_copy_score_later),
                color = BloxTheme.palette.label,
                style = BloxTheme.typography.small,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A score capped on 6 digits, as on the home and in the game, or « — » before any try. */
@Composable
private fun resultText(value: Int?): String =
    if (value == null) stringResource(R.string.feature_daily_no_value) else scoreText(value)

/** A cell of the first pieces: an I lying on its side fits the seventh of the panel of a phone of 360 dp. */
private val PIECE_CELL = 9.dp

/** The room of a piece: two cells, and the joints. */
private val PIECE_HEIGHT = 24.dp
