package ch.magori.blox.feature.game.ui

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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.frame.millisOf
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The card keeps the width of a phone on a larger screen. */
private val CardMaxWidth = 360.dp

/** The card rises from a quarter of its height under its place. */
private const val CARD_RISE_FRACTION = 4

/**
 * A card shown over the game (pause, game over): its [content], centred in a column, on the `scrim` that veils the game
 * and keeps its touches from it, and [over] it, what is drawn over the card and the scrim alike. It comes in as
 * `motion-card` says, by a fade of `motion-reduced-fade` in reduced effects; TalkBack announces it by its [title] as it
 * shows. It scrolls when the font is enlarged. A preview draws it in place, its way in done.
 */
@Composable
internal fun CardOverGame(
    title: String,
    modifier: Modifier = Modifier,
    over: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.card
    val still = LocalInspectionMode.current
    val shown = remember { MutableTransitionState(still).apply { targetState = true } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.scrim)
            .keepingTouches(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    content = content,
                )
            }
        }
        over()
    }
}

/** This part takes every touch that reaches it, so that none goes to the game under it. */
internal fun Modifier.keepingTouches(): Modifier =
    pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }

/** The title of a card over the game, a heading. */
@Composable
internal fun CardTitle(title: String) {
    Text(
        text = title,
        color = BloxTheme.palette.ink,
        fontSize = ScreenSizes.title,
        style = BloxTheme.typography.title,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
}

/** How the card comes in, with [effects]: it rises as it fades in (`motion-card`), or only fades when reduced. */
private fun cardEnter(effects: BloxEffects): EnterTransition = if (effects.reduced) {
    fadeIn(tween(millisOf(BloxMotion.reducedFade)))
} else {
    fadeIn(tween(millisOf(BloxMotion.card))) +
        slideInVertically(tween(millisOf(BloxMotion.card))) { it / CARD_RISE_FRACTION }
}
