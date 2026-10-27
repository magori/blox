package ch.magori.blox.feature.game.pause

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.frame.millisOf
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.sidepanel.levelText
import ch.magori.blox.feature.game.sidepanel.linesText
import ch.magori.blox.feature.game.sidepanel.scoreText
import ch.magori.blox.feature.game.ui.ScreenSizes
import ch.magori.blox.feature.game.ui.Statistic
import ch.magori.blox.feature.game.ui.valueDescription

/** The card keeps the width of a phone on a larger screen. */
private val CardMaxWidth = 360.dp

/** The card rises from a quarter of its height under its place. */
private const val CARD_RISE_FRACTION = 4

/**
 * The card of the pause (view V29), shown once the curtain hides the board: « Pause », the [score], [level] and
 * [lines], Reprendre ([onResume]) and Menu ([onMenu]), on the `scrim` that veils the game and keeps its touches from
 * it. It comes in as `motion-card` says, by a fade of `motion-reduced-fade` in reduced effects; TalkBack announces it
 * by its pane title as it shows. No mascot yet: none is drawn in core:designsystem. No logic here: the curtain and
 * the resume belong to the game screen.
 */
@Composable
internal fun PauseCard(
    score: Points,
    level: Level,
    lines: Lines,
    onResume: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.card
    val title = stringResource(R.string.feature_game_pause_title)
    // A preview draws the card in place, its way in done.
    val still = LocalInspectionMode.current
    val shown = remember { MutableTransitionState(still).apply { targetState = true } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.scrim)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .safeDrawingPadding()
            .padding(ScreenSizes.padding),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visibleState = shown, enter = cardEnter(BloxTheme.effects)) {
            Column(
                modifier = Modifier
                    .widthIn(max = CardMaxWidth)
                    .fillMaxWidth()
                    .semantics { paneTitle = title }
                    .background(palette.panel, shape)
                    .border(BloxBorders.frame, palette.panelBorder, shape)
                    .verticalScroll(rememberScrollState())
                    .padding(ScreenSizes.padding),
                verticalArrangement = Arrangement.spacedBy(ScreenSizes.gap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = palette.ink,
                    fontSize = ScreenSizes.title,
                    style = BloxTheme.typography.title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                CardStatistic(R.string.feature_game_score, scoreText(score), score.value)
                CardStatistic(R.string.feature_game_level, levelText(level), level.value)
                CardStatistic(R.string.feature_game_lines, linesText(lines), lines.value)
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

/** How the card comes in, with [effects]: it rises as it fades in (`motion-card`), or only fades when reduced. */
private fun cardEnter(effects: BloxEffects): EnterTransition = if (effects.reduced) {
    fadeIn(tween(millisOf(BloxMotion.reducedFade)))
} else {
    fadeIn(tween(millisOf(BloxMotion.card))) +
        slideInVertically(tween(millisOf(BloxMotion.card))) { it / CARD_RISE_FRACTION }
}

@Composable
private fun PausePreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        Box(modifier = Modifier.background(BloxTheme.palette.page)) {
            PauseCard(score = Points(12_340), level = Level(5), lines = Lines(52), onResume = {}, onMenu = {})
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
