package ch.magori.blox.feature.game.gameover

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.board.pieceColor
import ch.magori.blox.core.designsystem.component.PillBadge
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.GameLabels
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.sidepanel.scoreText
import ch.magori.blox.feature.game.ui.CardOverGame
import ch.magori.blox.feature.game.ui.CardTitle
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
 *
 * In Original, frozen, a screen of its own, in place of the game. In Colorful (V30), a card over the game, whose stack
 * has gone out (ADR 0024): the final score counts up as [play] says, and a new best throws its confetti over the card
 * and the scrim. [play] is read only while drawing, but by the score that counts, which recomposes its text alone.
 * Without [play], as in a preview, the score is shown whole, without confetti.
 */
@Composable
internal fun GameOverScreen(
    state: GameOverUiState,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    labels: GameLabels,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    play: () -> GameOverPlay? = NoPlay,
) {
    val title = stringResource(R.string.feature_game_over_title)
    if (BloxTheme.kind == BloxThemeKind.ORIGINAL) {
        TitledScreen(title = title, modifier = modifier) {
            GameOverContent(state, onReplay, onMenu, labels, onCopy, shownScore = null)
        }
    } else {
        // Derived: the score that counts recomposes only when the value it shows changes, not on every tick.
        val shownScore = remember(state, play) {
            val shown = derivedStateOf { play()?.shownScore(state.score) ?: state.score }
            return@remember { shown.value }
        }
        CardOverGame(title = title, modifier = modifier, over = { Confetti(play) }) {
            CardTitle(title)
            GameOverContent(state, onReplay, onMenu, labels, onCopy, shownScore)
        }
    }
}

/** No game over playing: the score is shown whole, without confetti. */
private val NoPlay: () -> GameOverPlay? = { null }

/**
 * What the game over shows under its title, as [GameOverScreen] says; the final score as [shownScore] gives it while it
 * counts up, if it does.
 */
@Composable
private fun ColumnScope.GameOverContent(
    state: GameOverUiState,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    labels: GameLabels,
    onCopy: () -> Unit,
    shownScore: (() -> Points)?,
) {
    val texts = gameOverTexts(state)
    if (state.newBest) NewBestBadge(stringResource(labels.gameOverNewBest))
    if (state.practice) PracticeNote()
    ScorePanel(
        texts = texts,
        bestLabel = stringResource(labels.gameOverBest(state.newBest)),
        shownScore = shownScore,
    )
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

/**
 * The final score in large, and under it the best before the game, if any ([GameOverTexts.previousBest]), under its
 * [bestLabel]: « Ancien record 098760 » once beaten, « Record 098760 » otherwise (V30, V39). TalkBack reads them as one,
 * at their real values: « Score : 123450, Ancien record : 98760 ». The score shown is the one [shownScore] gives while
 * it counts up, if it does: TalkBack reads its final value.
 */
@Composable
private fun ScorePanel(
    texts: GameOverTexts,
    bestLabel: String,
    shownScore: (() -> Points)?,
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
        if (shownScore == null) {
            ValueText(text = texts.score, maxFontSize = GameOverSizes.score)
        } else {
            CountingScore(shownScore)
        }
        if (previousBest != null) {
            Text(
                text = stringResource(R.string.feature_game_over_best, bestLabel, previousBest.shown),
                color = BloxTheme.palette.label,
                fontSize = GameOverSizes.previousBest,
                // On a narrow card in a large font, it goes on two lines, centred under the score.
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The final score as [shownScore] gives it, which counts up: read here, so that only this text recomposes as it counts
 * (ADR 0024).
 */
@Composable
private fun CountingScore(shownScore: () -> Points) {
    ValueText(text = scoreText(shownScore()), maxFontSize = GameOverSizes.score)
}

/**
 * The confetti of a new best that [play] throws, over the card and the scrim, in the colours of the pieces; read only
 * while drawing. TalkBack reads nothing of them.
 */
@Composable
private fun Confetti(play: () -> GameOverPlay?) {
    val variant = BloxTheme.variant
    val colours = remember(variant) { PieceType.entries.map { pieceColor(variant, it) } }
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val shown = play() ?: return@drawBehind
                val confetto = Size(ConfettoSize.width.toPx(), ConfettoSize.height.toPx())
                val centre = Offset(confetto.width / 2, confetto.height / 2)
                for (index in 0 until shown.confettiCount) {
                    val look = shown.confetto(index) ?: continue
                    translate(look.x * size.width - centre.x, look.y * size.height - centre.y) {
                        rotate(look.angle, pivot = centre) {
                            drawRect(colours[look.type.ordinal], size = confetto, alpha = look.alpha)
                        }
                    }
                }
            },
    )
}

/** A confetto, a small strip of paper. */
private val ConfettoSize = DpSize(8.dp, 5.dp)

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
