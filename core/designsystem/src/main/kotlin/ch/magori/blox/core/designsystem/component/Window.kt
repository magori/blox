package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
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

/**
 * A window: a panel of the theme, its [title] in [titleStyle], `screen-title` unless told otherwise, then its [content],
 * scrolling when the font is large. The title of a window goes over titleSmall, whose size Original keeps: its pixel font
 * stays as large as before Couleurs v2. The [titleModifier] lets a screen give its title the focus.
 */
@Composable
fun WindowPanel(
    title: String,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall.merge(BloxTheme.typography.screenTitle),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(WindowPadding),
        verticalArrangement = Arrangement.spacedBy(WindowGap),
    ) {
        PanelTitle(text = title, style = titleStyle, modifier = titleModifier)
        content()
    }
}

/**
 * The title of a panel (a window, a sheet): [text] in `ink` and in [style], read by TalkBack as a heading, cut at a
 * syllable, never in the middle of one, when a word is wider than the panel.
 */
@Composable
internal fun PanelTitle(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = BloxTheme.palette.ink,
        style = style.copy(hyphens = Hyphens.Auto),
        modifier = modifier.semantics { heading() },
    )
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
 * An answer of a window, a bubble button of its [kind] ([WindowButtonKind]): the safe one, Secondaire; the one the
 * window expects, Principal; the one that deletes, filled in `danger` beside the trash. Its [text] goes to the line
 * between its words only, centred; a word too wide shrinks, down to [shrinkFloor], rather than be cut. Disabled, it is
 * flat and announced as such.
 */
@Composable
fun WindowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: WindowButtonKind = WindowButtonKind.SECONDARY,
) {
    BubbleButton(family = kind.family, onClick = onClick, modifier = modifier, enabled = enabled) {
        kind.icon?.let { BloxIcon(it) }
        val style = LocalTextStyle.current.shrinkingLineHeight()
        Text(
            text = text,
            style = style,
            textAlign = TextAlign.Center,
            autoSize = shrinkToWholeWords(style.fontSize),
            // Off the edges of the pill when it goes to the line (« Prendre « X 2 » », twice as large).
            modifier = Modifier.weight(1f, fill = false).padding(vertical = BloxSpaces.space1),
        )
    }
}

/**
 * The two answers of a window, one above the other, each as wide as the window, read by TalkBack in this order: the
 * safe one first, [dismiss] (« Annuler » unless told otherwise), then [confirm], the answer the window expects, or the
 * one that deletes ([danger]); [confirm] is disabled unless [confirmEnabled], [dismiss] unless [dismissEnabled].
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
    dismissEnabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AnswerGap)) {
        WindowButton(
            text = dismiss,
            onClick = onDismiss,
            enabled = dismissEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
        WindowButton(
            text = confirm,
            onClick = onConfirm,
            enabled = confirmEnabled,
            kind = if (danger) WindowButtonKind.DANGER else WindowButtonKind.PRIMARY,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * What an answer of a window is: [SECONDARY], the safe one (Annuler, Garder…); [PRIMARY], the one the window expects;
 * [DANGER], the one that deletes, filled in `danger` beside the trash. Each a bubble button of its [family], with its
 * [icon], decorative, if it has one.
 */
enum class WindowButtonKind(internal val family: ButtonFamily, internal val icon: BloxIcon?) {
    SECONDARY(ButtonFamily.SECONDARY, null),
    PRIMARY(ButtonFamily.PRIMARY, null),
    DANGER(ButtonFamily.DANGER_FILLED, BloxIcon.TRASH),
}

/** Inside a window, between its parts, and between its two answers. */
private val WindowPadding = 12.dp
private val WindowGap = 12.dp
private val AnswerGap = BloxSpaces.space2
