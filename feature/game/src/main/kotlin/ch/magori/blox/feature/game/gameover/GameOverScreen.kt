package ch.magori.blox.feature.game.gameover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.LabelledPanel
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.ValueText

/** The sizes of the game over screen, in one place; those shared with the side column are in [PanelSizes]. */
private object GameOverSizes {
    /** Around the screen. */
    val screenPadding = 24.dp

    /** Between two blocks of the screen, and around the badge text horizontally. */
    val gap = 16.dp

    /** Around the badge text, vertically. */
    val smallGap = 8.dp

    /** The smallest touch target of a button (accessibility). */
    val buttonMinHeight = 48.dp
    val title = 22.sp

    /** The largest size of the score; it shrinks to stay on one line. */
    val score = 40.sp
}

/**
 * The game over screen (view V4): the title, the new record badge, the score in large, then the
 * lines, level, four-line clears and duration, and the buttons to replay or go back to the menu.
 * No logic here: the texts come from [gameOverTexts]. It is a state of the game screen: TalkBack
 * announces it by its pane title.
 */
@Composable
internal fun GameOverScreen(
    state: GameOverUiState,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = gameOverTexts(state)
    val palette = BloxTheme.palette
    val title = stringResource(R.string.feature_game_over_title)
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
            .background(palette.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(GameOverSizes.screenPadding),
        verticalArrangement = Arrangement.spacedBy(GameOverSizes.gap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = palette.ink,
            fontSize = GameOverSizes.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        if (state.newRecord) NewRecordBadge()
        val scoreLabel = stringResource(R.string.feature_game_score)
        LabelledPanel(
            label = scoreLabel,
            description = stringResource(R.string.feature_game_value_description, scoreLabel, texts.spokenScore),
            contentAlignment = Alignment.CenterHorizontally,
        ) {
            ValueText(text = texts.score, maxFontSize = GameOverSizes.score)
        }
        val linesLabel = stringResource(R.string.feature_game_lines)
        Statistic(
            label = linesLabel,
            value = texts.lines,
            description = stringResource(R.string.feature_game_value_description, linesLabel, texts.spokenLines),
        )
        val levelLabel = stringResource(R.string.feature_game_level)
        Statistic(
            label = levelLabel,
            value = texts.level,
            description = stringResource(R.string.feature_game_value_description, levelLabel, texts.spokenLevel),
        )
        Statistic(
            label = stringResource(R.string.feature_game_over_four_lines),
            value = texts.fourLineClears,
            description = stringResource(R.string.feature_game_over_four_lines_description, texts.spokenFourLineClears),
        )
        val durationLabel = stringResource(R.string.feature_game_over_duration)
        Statistic(
            label = durationLabel,
            value = texts.duration,
            description = stringResource(
                R.string.feature_game_value_description,
                durationLabel,
                spokenDuration(texts.spokenDuration),
            ),
        )
        Button(
            onClick = onReplay,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GameOverSizes.buttonMinHeight),
            shape = BloxTheme.shapes.panel,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentInk),
        ) {
            Text(text = stringResource(R.string.feature_game_over_replay))
        }
        OutlinedButton(
            onClick = onMenu,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GameOverSizes.buttonMinHeight),
            shape = BloxTheme.shapes.panel,
            border = BorderStroke(PanelSizes.border, palette.panelBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
        ) {
            Text(text = stringResource(R.string.feature_game_over_menu))
        }
    }
}

@Composable
private fun NewRecordBadge() {
    val palette = BloxTheme.palette
    Text(
        text = stringResource(R.string.feature_game_over_new_record).uppercase(),
        color = palette.accentInk,
        fontSize = PanelSizes.label,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(palette.accent, BloxTheme.shapes.panel)
            .padding(horizontal = GameOverSizes.gap, vertical = GameOverSizes.smallGap),
    )
}

/** One statistic on a line: its [label] and its [value]; TalkBack reads [description]. */
@Composable
private fun Statistic(
    label: String,
    value: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = BloxTheme.palette.label,
            fontSize = PanelSizes.label,
            modifier = Modifier.weight(1f, fill = false),
        )
        ValueText(text = value)
    }
}

/** The duration as TalkBack says it: "1 minute et 5 secondes", "1 minute" or "5 secondes", from [speech]. */
@Composable
@ReadOnlyComposable
private fun spokenDuration(speech: DurationSpeech): String {
    val minutes = speech.minutes?.let { pluralStringResource(R.plurals.feature_game_over_minutes, it, it) }
    val seconds = speech.seconds?.let { pluralStringResource(R.plurals.feature_game_over_seconds, it, it) }
    return if (minutes != null && seconds != null) {
        stringResource(R.string.feature_game_over_duration_both, minutes, seconds)
    } else {
        minutes ?: seconds.orEmpty()
    }
}

private val previewState = GameOverUiState(
    score = 123_450,
    lines = 87,
    level = 8,
    fourLineClears = 3,
    ticksPlayed = 45_300,
    newRecord = false,
)

@Composable
private fun GameOverPreview(
    kind: BloxThemeKind,
    newRecord: Boolean,
) {
    BloxTheme(kind = kind) {
        GameOverScreen(state = previewState.copy(newRecord = newRecord), onReplay = {}, onMenu = {})
    }
}

@Preview
@Composable
private fun GameOverOriginalPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newRecord = false)
}

@Preview
@Composable
private fun GameOverOriginalNewRecordPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newRecord = true)
}

@Preview
@Composable
private fun GameOverColorfulPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newRecord = false)
}

@Preview
@Composable
private fun GameOverColorfulNewRecordPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newRecord = true)
}

@Preview(fontScale = 2f)
@Composable
private fun GameOverLargeFontPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newRecord = true)
}
