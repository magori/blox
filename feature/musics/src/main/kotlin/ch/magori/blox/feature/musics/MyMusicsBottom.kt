package ch.magori.blox.feature.musics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.component.SnackMessage
import ch.magori.blox.core.designsystem.component.buttonLabelStyle
import ch.magori.blox.core.designsystem.component.holdSideBySide
import ch.magori.blox.core.designsystem.component.sidePadding
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * [content] in all the room it is given, and [bottom] over its foot, drawn above it. [content] learns how high
 * [bottom] is, the room to leave at its end so that its last line can scroll out from under it.
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
            bottoms.forEach { it.place(0, constraints.maxHeight - it.height) }
        }
    }
}

/** The parts of [AboveBottomActions]. */
private enum class BottomSlot { CONTENT, BOTTOM }

/**
 * The foot of the page, on a fade from the page seen through to the page: « « X » est supprimée. » with *Annuler* and
 * its cross while the music [deleted] last may come back, then *Au hasard* and *Nouvelle* ([EditorButtons]).
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
    val random = FootButton(ButtonFamily.SECONDARY, BloxIcon.DICE, stringResource(R.string.feature_musics_random))
    val new = FootButton(ButtonFamily.PRIMARY, BloxIcon.PLUS, stringResource(R.string.feature_musics_new))
    val measurer = rememberTextMeasurer()
    val styles = listOf(random, new).map { buttonLabelStyle(it.family) }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sideBySide = with(density) {
            val beside = (IconSizes.besideLabel + IconSizes.gap).roundToPx()
            holdSideBySide(
                width = constraints.maxWidth,
                gap = BloxSpaces.space2.roundToPx(),
                padding = ButtonFamily.SECONDARY.sidePadding.roundToPx(),
                weights = listOf(1f, 1f),
                labels = listOf(random, new).zip(styles) { button, style ->
                    beside + measurer.measure(button.label, style, softWrap = false, maxLines = 1).size.width
                },
            )
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
                FootButton(random, onRandom, Modifier.weight(1f))
                FootButton(new, onNew, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
                FootButton(random, onRandom, Modifier.fillMaxWidth())
                FootButton(new, onNew, Modifier.fillMaxWidth())
            }
        }
    }
}

/** A button of the foot: its [family], its [icon] and its [label]. */
@Immutable
private data class FootButton(val family: ButtonFamily, val icon: BloxIcon, val label: String)

/** The [button] of the foot: its icon, decorative, then its label on one line, shrunk if it must. */
@Composable
private fun FootButton(
    button: FootButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BubbleButton(family = button.family, onClick = onClick, modifier = modifier) {
        BloxIcon(button.icon, Modifier.testTag(MUSICS_ICON_TAG))
        SingleLineText(text = button.label, style = LocalTextStyle.current)
    }
}

/** How high the fade over the list is, above the foot of the page. */
private val FADE = BloxSpaces.space6
