package ch.magori.blox.feature.game.gameover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.LabelledPanel
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.PrimaryButton
import ch.magori.blox.feature.game.ui.ScreenSizes
import ch.magori.blox.feature.game.ui.SecondaryButton
import ch.magori.blox.feature.game.ui.TitledScreen
import ch.magori.blox.feature.game.ui.ValueText

/** The sizes of the game over screen, in one place; those shared with the side column are in [PanelSizes]. */
private object GameOverSizes {
    /** Around the badge text, vertically; horizontally, it is [ScreenSizes.gap]. */
    val smallGap = 8.dp

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
    TitledScreen(title = stringResource(R.string.feature_game_over_title), modifier = modifier) {
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
        PrimaryButton(
            text = stringResource(R.string.feature_game_over_replay),
            icon = BloxIcon.REPLAY,
            onClick = onReplay,
        )
        SecondaryButton(
            text = stringResource(R.string.feature_game_over_menu),
            icon = BloxIcon.HOME,
            onClick = onMenu,
        )
    }
}

@Composable
private fun NewRecordBadge() {
    val palette = BloxTheme.palette
    Text(
        text = stringResource(R.string.feature_game_over_new_record).uppercase(),
        color = palette.accentInk,
        fontSize = PanelSizes.label,
        style = BloxTheme.typography.title,
        modifier = Modifier
            .background(palette.accent, BloxTheme.shapes.pill)
            .padding(horizontal = ScreenSizes.gap, vertical = GameOverSizes.smallGap),
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
    score = Points(123_450),
    lines = Lines(87),
    level = Level(8),
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

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview
@Composable
internal fun GameOverOriginalPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newRecord = false)
}

@Preview
@Composable
internal fun GameOverOriginalNewRecordPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newRecord = true)
}

@Preview
@Composable
internal fun GameOverColorfulPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newRecord = false)
}

@Preview
@Composable
internal fun GameOverColorfulNewRecordPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newRecord = true)
}

@Preview(fontScale = 2f)
@Composable
internal fun GameOverLargeFontPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newRecord = true)
}
