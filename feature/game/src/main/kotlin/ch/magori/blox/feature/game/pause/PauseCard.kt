package ch.magori.blox.feature.game.pause

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.mascot.Mascot
import ch.magori.blox.core.board.mascot.MascotCharacter
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.music.LiveMood
import ch.magori.blox.feature.game.sidepanel.levelText
import ch.magori.blox.feature.game.sidepanel.linesText
import ch.magori.blox.feature.game.sidepanel.scoreText
import ch.magori.blox.feature.game.ui.CardOverGame
import ch.magori.blox.feature.game.ui.CardTitle
import ch.magori.blox.feature.game.ui.Statistic
import ch.magori.blox.feature.game.ui.valueDescription

/** The tag of the mascot of the card, decorative, read by the tests only. */
internal const val PAUSE_MASCOT_TAG = "pause_mascot"

/** The cells of the mascot of the card on a phone (docs/design-system/pictogrammes.md, Mascotte). */
private val MascotCell = 16.dp

/** Room above the mascot for its « z », which rise over its top. */
private val MascotRoom = 12.dp

/**
 * The card of the pause (view V29), shown once the curtain hides the board: « Pause », the [score], [level] and
 * [lines], the mood of the live music when the game plays it ([liveMood]: « Vivante · calme » or « Vivante · tendue »,
 * V37), Reprendre ([onResume]) and Menu ([onMenu]), a [CardOverGame]. In Colorful, the Classic mascot sleeps above
 * « Pause », a decoration; Original, frozen, has none. No logic here: the curtain and the resume belong to the game
 * screen.
 */
@Composable
internal fun PauseCard(
    score: Points,
    level: Level,
    lines: Lines,
    liveMood: LiveMood?,
    onResume: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.feature_game_pause_title)
    CardOverGame(title = title, modifier = modifier) {
        if (BloxTheme.variant != BloxThemeVariant.ORIGINAL) {
            Mascot(
                character = MascotCharacter.CLASSIC,
                cellSize = MascotCell,
                asleep = true,
                modifier = Modifier.padding(top = MascotRoom).testTag(PAUSE_MASCOT_TAG),
            )
        }
        CardTitle(title)
        CardStatistic(R.string.feature_game_score, scoreText(score), score.value)
        CardStatistic(R.string.feature_game_level, levelText(level), level.value)
        CardStatistic(R.string.feature_game_lines, linesText(lines), lines.value)
        if (liveMood != null) LiveMoodStatistic(liveMood)
        // The tag is read by the measured journey of `:benchmark` (`By.res`), whatever the language.
        PrimaryButton(
            text = stringResource(R.string.feature_game_pause_resume),
            icon = BloxIcon.PLAY,
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().testTag("pause_resume"),
        )
        SecondaryButton(
            text = stringResource(R.string.feature_game_pause_menu),
            icon = BloxIcon.HOME,
            onClick = onMenu,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A value of the card, its [value] beside its label ([labelId]): TalkBack reads the label and the [rawValue]. */
@Composable
private fun CardStatistic(
    @StringRes labelId: Int,
    value: String,
    rawValue: Int,
) {
    val label = stringResource(labelId)
    Statistic(
        label = label,
        value = value,
        description = valueDescription(label, rawValue.toString()),
    )
}

/**
 * The live music of the game in [mood], beside « Musique », as a value of the card: « Vivante · calme »; TalkBack reads
 * « Musique : Vivante, calme », without the dot.
 */
@Composable
private fun LiveMoodStatistic(mood: LiveMood) {
    val label = stringResource(R.string.feature_game_pause_music)
    val (text, description) = when (mood) {
        LiveMood.CALM -> R.string.feature_game_pause_live_calm to R.string.feature_game_pause_live_calm_description
        LiveMood.TENSE -> R.string.feature_game_pause_live_tense to R.string.feature_game_pause_live_tense_description
    }
    Statistic(
        label = label,
        value = stringResource(text),
        description = valueDescription(label, stringResource(description)),
        valueGivesWay = true,
    )
}

/** The card in [kind], of a game of the live music in [liveMood], if any. */
@Composable
private fun PausePreview(
    kind: BloxThemeKind,
    liveMood: LiveMood? = null,
) {
    BloxTheme(kind = kind) {
        Box(modifier = Modifier.background(BloxTheme.palette.page)) {
            PauseCard(
                score = Points(12_340),
                level = Level(5),
                lines = Lines(52),
                liveMood = liveMood,
                onResume = {},
                onMenu = {},
            )
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview
@Composable
internal fun PauseOriginalPreview() {
    PausePreview(BloxThemeKind.ORIGINAL)
}

@Preview
@Composable
internal fun PauseColorfulPreview() {
    PausePreview(BloxThemeKind.COLORFUL)
}

@Preview(fontScale = 2f)
@Composable
internal fun PauseLargeFontPreview() {
    PausePreview(BloxThemeKind.ORIGINAL)
}

@Preview
@Composable
internal fun PauseLiveCalmPreview() {
    PausePreview(BloxThemeKind.COLORFUL, LiveMood.CALM)
}

@Preview
@Composable
internal fun PauseLiveTensePreview() {
    PausePreview(BloxThemeKind.COLORFUL, LiveMood.TENSE)
}

@Preview(fontScale = 2f)
@Composable
internal fun PauseLiveLargeFontPreview() {
    PausePreview(BloxThemeKind.COLORFUL, LiveMood.TENSE)
}
