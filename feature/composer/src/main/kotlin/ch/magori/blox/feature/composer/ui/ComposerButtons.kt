package ch.magori.blox.feature.composer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.FramedBox
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.component.WrappingLabel
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The sizes shared by the parts of the editor, in one place. */
internal object ComposerSizes {
    /** Around the screen. */
    val padding = 12.dp

    /** Between two blocks of the screen, and between two buttons. */
    val gap = 4.dp
}

/**
 * A button of the editor, framed as a panel: [text] on it, [description] read by TalkBack in its place when the text
 * alone is too short to say what it does. Disabled when it has no effect: greyed out, and announced as such. A
 * [primary] button, *Enregistrer* with changes to save, is filled in `accent`. A label that [wraps] goes on several
 * lines when it does not fit, rather than shrinking. A button as wide as its label, rather than given its width,
 * leaves [labelPadding] on each side of it. Its [icon], decorative, goes above or before the label. The answers of a
 * window are `WindowButton` (core:designsystem).
 */
@Composable
internal fun ToolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    primary: Boolean = false,
    wraps: Boolean = false,
    labelPadding: Dp = 0.dp,
    icon: ToolIcon? = null,
) {
    val palette = BloxTheme.palette
    FramedBox(
        background = if (primary) palette.accent else palette.panel,
        frame = if (primary) palette.accent else palette.panelBorder,
        labelPadding = labelPadding,
        modifier = modifier
            .alpha(if (enabled) 1f else BloxAlphas.DISABLED)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .described(description),
    ) {
        val ink = if (primary) palette.accentInk else palette.ink
        IconAndLabel(icon, ink) {
            if (wraps) WrappingLabel(text = text, color = ink) else ButtonLabel(text = text, color = ink)
        }
    }
}

/**
 * A choice of the editor among others (a tab, the mode of the touches, a bar), read by TalkBack with its [role]: a
 * selected one is filled in [selectedColor], `accent` unless told otherwise, its label in [selectedInk]; an unselected
 * one is framed. In every theme the two differ by 3:1 at least in luminance (`TrackColorsTest` checks it for the tabs),
 * so that hue is not their only cue. A choice with a [mark] (the bar played) shows a line under its label, in the
 * colour of the label, and TalkBack reads the mark as its state. A choice as wide as its label, rather than given its
 * width, leaves [labelPadding] on each side of it. Its [icon], decorative, goes above or before the label.
 */
@Composable
internal fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    role: Role,
    modifier: Modifier = Modifier,
    description: String? = null,
    mark: String? = null,
    selectedColor: Color = BloxTheme.palette.accent,
    selectedInk: Color = BloxTheme.palette.accentInk,
    labelPadding: Dp = 0.dp,
    icon: ToolIcon? = null,
) {
    val palette = BloxTheme.palette
    val background = if (selected) selectedColor else palette.panel
    val ink = if (selected) selectedInk else palette.ink
    FramedBox(
        background = background,
        frame = if (selected) background else palette.panelBorder,
        labelPadding = labelPadding,
        modifier = modifier
            .selectable(selected = selected, role = role, onClick = onClick)
            .described(description)
            .marked(mark, ink),
    ) {
        IconAndLabel(icon, ink) { ButtonLabel(text = text, color = ink) }
    }
}

/** An icon of a button of the editor: [icon], [above] its label, as in the toolbar, or before it. */
@Immutable
internal data class ToolIcon(val icon: BloxIcon, val above: Boolean)

/**
 * [label] with its [icon] in [ink], if any: above it, [IconSizes.aboveLabel] high, or before it, [IconSizes.besideLabel]
 * high, [IconSizes.denseGap] apart, the label wrapping in the room left.
 */
@Composable
private fun IconAndLabel(
    icon: ToolIcon?,
    ink: Color,
    label: @Composable () -> Unit,
) {
    when {
        icon == null -> label()

        icon.above -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BloxIcon(icon.icon, size = IconSizes.aboveLabel, tint = ink)
            label()
        }

        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            BloxIcon(icon.icon, tint = ink)
            Spacer(modifier = Modifier.width(IconSizes.denseGap))
            Box(modifier = Modifier.weight(1f, fill = false)) { label() }
        }
    }
}

/** The line of a [mark] under the label, in [ink], and the mark read by TalkBack as the state of the button. */
private fun Modifier.marked(
    mark: String?,
    ink: Color,
): Modifier = if (mark == null) {
    this
} else {
    semantics { stateDescription = mark }.drawWithContent {
        drawContent()
        val width = size.width * MARK_WIDTH
        val height = MarkHeight.toPx()
        drawRect(
            color = ink,
            topLeft = Offset((size.width - width) / 2, size.height - height - MarkInset.toPx()),
            size = Size(width, height),
        )
    }
}

/** The line of a mark: this part of the width of the button, this high, this far above its bottom edge. */
private const val MARK_WIDTH = 0.5f
private val MarkHeight = 3.dp
private val MarkInset = 6.dp

/** The label of a button, on one line, shrunk if needed. */
@Composable
private fun ButtonLabel(
    text: String,
    color: Color,
) {
    SingleLineText(text = text, color = color, style = buttonLabelStyle, textAlign = TextAlign.Center)
}

/** The style of the label of a button; the toolbar measures its labels with it. */
internal val buttonLabelStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography.labelMedium.copy(lineHeight = TextUnit.Unspecified)

/** TalkBack reads [description], when there is one, in place of the label of the button. */
private fun Modifier.described(description: String?): Modifier =
    if (description == null) this else semantics { contentDescription = description }

/**
 * Radio buttons among [choices], each named by [label], the [selected] one filled; a touch chooses its choice through
 * [onChosen]. They stand on one line, each sized by [choiceModifier], [labelPadding] on each side of its label, or, when
 * they [scroll], each as wide as its label,
 * no wider than the line, the line scrolling sideways when they do not fit, the one selected brought in sight.
 */
@Composable
internal fun <T> ChoiceGroup(
    choices: List<T>,
    selected: T,
    onChosen: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    scroll: Boolean = false,
    labelPadding: Dp = 0.dp,
    choiceModifier: RowScope.(T) -> Modifier = { Modifier },
    icon: (T) -> ToolIcon? = { null },
) {
    // A line of choices, each sized by [sizing].
    val line: @Composable (Modifier, Modifier) -> Unit = { lineModifier, sizing ->
        Row(
            modifier = lineModifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
        ) {
            choices.forEach { choice ->
                val isSelected = choice == selected
                // The choice made comes in sight on a line that scrolls, whole.
                val sight = remember { BringIntoViewRequester() }
                LaunchedEffect(isSelected, scroll) { if (isSelected && scroll) sight.bringIntoView() }
                ChoiceButton(
                    text = label(choice),
                    selected = isSelected,
                    onClick = { onChosen(choice) },
                    role = Role.RadioButton,
                    labelPadding = if (scroll) ScrolledLabelPadding else labelPadding,
                    icon = icon(choice),
                    modifier = choiceModifier(choice).bringIntoViewRequester(sight).then(sizing),
                )
            }
        }
    }
    if (scroll) {
        // A choice never wider than the line, its label shrinking to fit: in sight, it shows whole.
        BoxWithConstraints(modifier = modifier) {
            line(
                Modifier.horizontalScroll(rememberScrollState()),
                Modifier.widthIn(min = BloxSizes.minTouchTarget, max = maxOf(maxWidth, BloxSizes.minTouchTarget)),
            )
        }
    } else {
        line(modifier, Modifier)
    }
}

/** The room on each side of the label of a choice as wide as its label. */
private val ScrolledLabelPadding = 6.dp
