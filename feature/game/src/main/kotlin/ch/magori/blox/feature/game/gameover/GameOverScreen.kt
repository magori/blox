package ch.magori.blox.feature.game.gameover

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.component.PillBadge
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.GameLabels
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ui.LabelledPanel
import ch.magori.blox.feature.game.ui.PanelSizes
import ch.magori.blox.feature.game.ui.ScreenSizes
import ch.magori.blox.feature.game.ui.Statistic
import ch.magori.blox.feature.game.ui.TitledScreen
import ch.magori.blox.feature.game.ui.ValueText
import ch.magori.blox.feature.game.ui.valueDescription

/** The sizes of the game over screen, in one place; those shared with the side column are in [PanelSizes]. */
private object GameOverSizes {
    /** Around the badge text, vertically; horizontally, it is [ScreenSizes.gap]. */
    val smallGap = 8.dp

    /** The largest size of the score; it shrinks to stay on one line. */
    val score = 40.sp

    /** The best before the game, under the score. */
    val previousBest = 14.sp
}

/**
 * The game over screen (views V4, V30): the title, the new best badge, the score in large with the best before the
 * game under it, then the lines, level, four-line clears and duration, and the buttons to replay or go back to the menu.
 * No logic here: the texts come from [gameOverTexts]. It is a state of the game screen: TalkBack
 * announces it by its pane title. The badge is the one of the variant of the game ([labels]): MEILLEUR DU JOUR for a
 * daily game (V39), whose game over also offers Copier, between Rejouer and Menu, given to [onCopy]; TalkBack reads it
 * « Copier mon score », what it does, where the button shows the short label of V39. Not when the results kept do not
 * hold this game ([GameOverUiState.resultsKept]): Copier would copy them without it, or nothing. A daily game not
 * counted, its date already passed ([GameOverUiState.practice]), says so under the title: « Partie d’entraînement ».
 */
@Composable
internal fun GameOverScreen(
    state: GameOverUiState,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    labels: GameLabels,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = gameOverTexts(state)
    TitledScreen(title = stringResource(R.string.feature_game_over_title), modifier = modifier) {
        if (state.newBest) NewBestBadge(stringResource(labels.gameOverNewBest))
        if (state.practice) PracticeNote()
        ScorePanel(texts = texts, bestLabel = stringResource(labels.gameOverBest(state.newBest)))
        val linesLabel = stringResource(R.string.feature_game_lines)
        Statistic(
            label = linesLabel,
            value = texts.lines,
            description = valueDescription(linesLabel, texts.spokenLines),
        )
        val levelLabel = stringResource(R.string.feature_game_level)
        Statistic(
            label = levelLabel,
            value = texts.level,
            description = valueDescription(levelLabel, texts.spokenLevel),
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
            description = valueDescription(durationLabel, spokenDuration(texts.spokenDuration)),
        )
        PrimaryButton(
            text = stringResource(R.string.feature_game_over_replay),
            icon = BloxIcon.REPLAY,
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth(),
        )
        if (labels.copyOffered && state.resultsKept) {
            val copyDescription = stringResource(R.string.feature_game_over_copy_description)
            SecondaryButton(
                text = stringResource(R.string.feature_game_over_copy),
                icon = BloxIcon.COPY,
                onClick = onCopy,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = copyDescription },
            )
        }
        SecondaryButton(
            text = stringResource(R.string.feature_game_over_menu),
            icon = BloxIcon.HOME,
            onClick = onMenu,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The final score in large, and under it the best before the game, if any ([GameOverTexts.previousBest]), under its
 * [bestLabel]: « Ancien record 098760 » once beaten, « Record 098760 » otherwise (V30, V39). TalkBack reads them as one,
 * at their real values: « Score : 123450, Ancien record : 98760 ».
 */
@Composable
private fun ScorePanel(
    texts: GameOverTexts,
    bestLabel: String,
) {
    val scoreLabel = stringResource(R.string.feature_game_score)
    val scoreDescription = valueDescription(scoreLabel, texts.spokenScore)
    val previousBest = texts.previousBest
    LabelledPanel(
        label = scoreLabel,
        description = previousBest?.let {
            stringResource(
                R.string.feature_game_over_score_and_best,
                scoreDescription,
                valueDescription(bestLabel, it.spoken),
            )
        } ?: scoreDescription,
        contentAlignment = Alignment.CenterHorizontally,
    ) {
        ValueText(text = texts.score, maxFontSize = GameOverSizes.score)
        if (previousBest != null) {
            Text(
                text = stringResource(R.string.feature_game_over_best, bestLabel, previousBest.shown),
                color = BloxTheme.palette.label,
                fontSize = GameOverSizes.previousBest,
            )
        }
    }
}

/**
 * The game was only practice ([GameOverUiState.practice]): a daily game replayed once the day changed, whose try is
 * not counted (P9.9). A plain line in the colour of the labels, not a badge: it is no achievement.
 */
@Composable
private fun PracticeNote() {
    Text(
        text = stringResource(R.string.feature_game_over_practice),
        color = BloxTheme.palette.label,
        fontSize = PanelSizes.label,
        style = BloxTheme.typography.title,
    )
}

@Composable
private fun NewBestBadge(text: String) {
    PillBadge(
        text = text.uppercase(),
        style = BloxTheme.typography.title.copy(fontSize = PanelSizes.label),
        // On a narrow phone in a large font, RECORD goes to a line of its own rather than vanish.
        maxLines = Int.MAX_VALUE,
        padding = PaddingValues(horizontal = ScreenSizes.gap, vertical = GameOverSizes.smallGap),
    )
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
    newBest = false,
    resultsKept = true,
    previousBest = 0,
    practice = false,
)

/**
 * The game over in [kind], with the best before the game ([previousBest]) recalled under the score; none in the
 * Original previews: that theme no longer changes, its captures stay as they were.
 */
@Composable
private fun GameOverPreview(
    kind: BloxThemeKind,
    newBest: Boolean,
    labels: GameLabels,
    previousBest: Int = 0,
    practice: Boolean = false,
) {
    BloxTheme(kind = kind) {
        GameOverScreen(
            state = previewState.copy(
                newBest = newBest,
                previousBest = previousBest,
                practice = practice,
                resultsKept = !practice,
            ),
            onReplay = {},
            onMenu = {},
            labels = labels,
            onCopy = {},
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview
@Composable
internal fun GameOverOriginalPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newBest = false, labels = GameLabels.Classic)
}

@Preview
@Composable
internal fun GameOverOriginalNewRecordPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newBest = true, labels = GameLabels.Classic)
}

@Preview
@Composable
internal fun GameOverColorfulPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newBest = false, labels = GameLabels.Classic, previousBest = 150_000)
}

@Preview
@Composable
internal fun GameOverColorfulNewRecordPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newBest = true, labels = GameLabels.Classic, previousBest = 98_760)
}

@Preview(fontScale = 2f)
@Composable
internal fun GameOverLargeFontPreview() {
    GameOverPreview(BloxThemeKind.ORIGINAL, newBest = true, labels = GameLabels.Classic)
}

/** The game over of a daily game (V39): MEILLEUR DU JOUR, then Rejouer, Copier and Menu; in the Colorful theme only. */
@Preview
@Composable
internal fun GameOverDailyNewBestPreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newBest = true, labels = GameLabels.Daily, previousBest = 98_760)
}

/** The game over of a daily game not counted, its date already passed (P9.9): « Partie d’entraînement », no Copier. */
@Preview
@Composable
internal fun GameOverDailyPracticePreview() {
    GameOverPreview(BloxThemeKind.COLORFUL, newBest = false, labels = GameLabels.Daily, practice = true)
}
