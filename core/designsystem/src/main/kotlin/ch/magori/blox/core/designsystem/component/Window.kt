package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.designsystem.theme.shrinkingLineHeight

/*
 * The windows of the screens, over them until they are answered or closed: a panel of the theme, its title, its
 * sentences and its answers. Their texts are the screen's; only the words every window says (« Annuler »,
 * « L'opération a échoué. »…) are here.
 */

/**
 * A window over the screen, its [content] (a [WindowPanel]) [BloxSizes.windowMargin] from the edges of the screen:
 * wider than the default of the platform, so that a title in the pixel font, twice as large, keeps its words whole.
 * Back, or a touch outside it, calls [onDismissRequest].
 */
@Composable
fun WindowDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = modifier.padding(horizontal = BloxSizes.windowMargin)) { content() }
    }
}

/** A window: a panel of the theme, its [title], then its [content], scrolling when the font is large. */
@Composable
fun WindowPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(WindowPadding),
        verticalArrangement = Arrangement.spacedBy(WindowGap),
    ) {
        Text(
            text = title,
            color = palette.ink,
            // Cut at a syllable, never in the middle of one, when a word is wider than the window.
            style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleSmall).copy(hyphens = Hyphens.Auto),
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * A sentence of a window, going to the line between its words only: a word wider than the line, its punctuation with
 * it, shrinks (down to [shrinkFloor]) rather than be cut, which would leave a final point alone on its line.
 */
@Composable
fun WindowText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.bodyMedium.shrinkingLineHeight()
    Text(
        text = text,
        color = BloxTheme.palette.ink,
        style = style,
        autoSize = shrinkToWholeWords(style.fontSize),
        modifier = modifier,
    )
}

/** « L'opération a échoué. », in `danger`, read by TalkBack as it shows: the window stays open to try again. */
@Composable
fun WindowFailure(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.core_designsystem_window_failed),
        color = BloxTheme.palette.danger,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * An answer of a window, framed as a panel, its [text] on as many lines as it needs ([WrappingLabel]). The answer
 * the window expects, [primary], is filled in `accent`; the one that deletes, [danger], in `danger`. Disabled, it is
 * greyed out and announced as such.
 */
@Composable
fun WindowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    danger: Boolean = false,
) {
    val palette = BloxTheme.palette
    val fill = when {
        danger -> palette.danger
        primary -> palette.accent
        else -> null
    }
    FramedBox(
        background = fill ?: palette.panel,
        frame = fill ?: palette.panelBorder,
        labelPadding = AnswerLabelPadding,
        modifier = modifier
            .alpha(if (enabled) 1f else BloxAlphas.DISABLED)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        WrappingLabel(
            text = text,
            color = when {
                danger -> palette.dangerInk
                primary -> palette.accentInk
                else -> palette.ink
            },
        )
    }
}

/**
 * The two answers of a window: [dismiss] (« Annuler » unless told otherwise), then [confirm], filled in `accent`, or in
 * `danger` when it deletes ([danger]); [confirm] is disabled unless [confirmEnabled]. Side by side when each word of
 * their labels holds whole in half the width; one above the other otherwise, in the same order, rather than cut a word
 * or shrink a font made larger (« Annuler » in the pixel font, twice as large, on 360 dp).
 */
@Composable
fun WindowAnswers(
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismiss: String = stringResource(R.string.core_designsystem_window_cancel),
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
) {
    val answers: @Composable (Modifier) -> Unit = { sizing ->
        WindowButton(text = dismiss, onClick = onDismiss, modifier = sizing)
        WindowButton(
            text = confirm,
            onClick = onConfirm,
            enabled = confirmEnabled,
            primary = true,
            danger = danger,
            modifier = sizing,
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val style = wrappingLabelStyle
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val widest = remember(style, density, dismiss, confirm) {
            listOf(measurer.widestWord(dismiss, style), measurer.widestWord(confirm, style))
        }
        val sideBySide = with(density) {
            holdSideBySide(
                width = constraints.maxWidth,
                gap = AnswerGap.roundToPx(),
                padding = framedBoxPaddingPx(AnswerLabelPadding),
                weights = listOf(1f, 1f),
                labels = widest,
            )
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(AnswerGap)) { answers(Modifier.weight(1f)) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(WindowGap)) { answers(Modifier.fillMaxWidth()) }
        }
    }
}

/** Inside a window, between its parts, and between its two answers. */
private val WindowPadding = 12.dp
private val WindowGap = 12.dp
private val AnswerGap = 4.dp

/** On each side of the label of an answer, within its frame. */
private val AnswerLabelPadding = 0.dp
