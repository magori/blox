package ch.magori.blox.feature.musics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.MessageAction
import ch.magori.blox.core.designsystem.component.SideBySideOrStacked
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.component.SnackMessage
import ch.magori.blox.core.designsystem.component.buttonLabelStyle
import ch.magori.blox.core.designsystem.component.lineWidth
import ch.magori.blox.core.designsystem.component.sidePadding
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * [content] in all the room it is given, and [bottom] over its foot, drawn above it. [content] learns how high
 * [bottom] is, the room to leave at its end so that its last line can scroll out from under it. [bottom] gets no more
 * than the whole room, from its top: what does not hold there must scroll within it.
 */
@Composable
internal fun AboveBottomActions(
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (bottomRoom: Dp) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val bottoms = subcompose(BottomSlot.BOTTOM, bottom).map { it.measure(loose) }
        val bottomHeight = bottoms.maxOfOrNull { it.height } ?: 0
        val contents = subcompose(BottomSlot.CONTENT) { content(bottomHeight.toDp()) }.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            contents.forEach { it.place(0, 0) }
            bottoms.forEach { it.place(0, (constraints.maxHeight - it.height).coerceAtLeast(0)) }
        }
    }
}

/** The parts of [AboveBottomActions]. */
private enum class BottomSlot { CONTENT, BOTTOM }

/**
 * The foot of the page, on a fade from the page seen through to the page: « « X » est supprimée. » with *Annuler* and
 * its cross while the music [deleted] last may come back, then *Au hasard* and *Nouvelle* ([EditorButtons]). Higher
 * than the room it is given, as in a large font on a low window, it scrolls.
 */
@Composable
internal fun BottomActions(
    deleted: String?,
    actions: MyMusicsActions,
    modifier: Modifier = Modifier,
) {
    val page = BloxTheme.palette.page
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val solid = (FADE.toPx() / size.height).coerceAtMost(1f)
                drawRect(Brush.verticalGradient(0f to page.copy(alpha = 0f), solid to page))
            }
            .verticalScroll(rememberScrollState())
            .padding(top = FADE, bottom = BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
    ) {
        if (deleted != null) {
            SnackMessage(
                text = stringResource(R.string.feature_musics_deleted, deleted),
                action = MessageAction(stringResource(R.string.feature_musics_undo), actions::onUndo),
                onClose = actions::onUndoDismissed,
            )
        }
        EditorButtons(onRandom = actions::onRandom, onNew = actions::onNew)
    }
}

/**
 * *Au hasard*, Secondaire, after its die, and *Nouvelle*, Principal, after its plus: side by side while each label holds
 * on one line beside its icon in its half; one above the other otherwise, as in a large font or in the pixel font.
 */
@Composable
private fun EditorButtons(
    onRandom: () -> Unit,
    onNew: () -> Unit,
) {
    val buttons = listOf(
        FootButton(ButtonFamily.SECONDARY, BloxIcon.DICE, stringResource(R.string.feature_musics_random), onRandom),
        FootButton(ButtonFamily.PRIMARY, BloxIcon.PLUS, stringResource(R.string.feature_musics_new), onNew),
    )
    val measurer = rememberTextMeasurer()
    val styles = buttons.map { buttonLabelStyle(it.family) }
    val density = LocalDensity.current
    val labels = buttons.map { it.label }
    // A label on one line needs its own width, and its icon with the gap after it.
    val widths = remember(measurer, styles, labels, density) {
        val beside = with(density) { (IconSizes.besideLabel + IconSizes.gap).roundToPx() }
        labels.zip(styles) { label, style -> beside + measurer.lineWidth(label, style) }
    }
    SideBySideOrStacked(
        labelWidths = widths,
        padding = ButtonFamily.SECONDARY.sidePadding,
        gap = BloxSpaces.space2,
    ) { index, sizing -> FootButton(buttons[index], sizing) }
}

/** A button of the foot: its [family], its [icon], its [label], and what touching it does, [onClick]. */
@Immutable
private class FootButton(val family: ButtonFamily, val icon: BloxIcon, val label: String, val onClick: () -> Unit)

/**
 * The [button] of the foot: its icon, decorative, then its label on one line, shrunk only if it must. Not a
 * `PrimaryButton` nor a `SecondaryButton`, whose labels go to the line: stacked, in the pixel font twice as large,
 * « Au hasard » after its die needs all but a few pixels of the line, and must not break.
 */
@Composable
private fun FootButton(
    button: FootButton,
    modifier: Modifier = Modifier,
) {
    BubbleButton(family = button.family, onClick = button.onClick, modifier = modifier) {
        BloxIcon(button.icon, Modifier.testTag(MUSICS_ICON_TAG))
        SingleLineText(text = button.label, style = LocalTextStyle.current)
    }
}

/** How high the fade over the list is, above the foot of the page. */
private val FADE = BloxSpaces.space6
