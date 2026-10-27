package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.framedBoxPaddingPx
import ch.magori.blox.core.designsystem.component.holdSideBySide
import ch.magori.blox.core.designsystem.component.labelsHold
import ch.magori.blox.core.designsystem.component.rowShares
import ch.magori.blox.core.designsystem.component.widestWord
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines
import ch.magori.blox.core.designsystem.theme.shrinkingLineHeight
import ch.magori.blox.feature.composer.grid.trackColors
import ch.magori.blox.feature.composer.model.BARS
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.ui.ChoiceButton
import ch.magori.blox.feature.composer.ui.ChoiceGroup
import ch.magori.blox.feature.composer.ui.ComposerSizes
import ch.magori.blox.feature.composer.ui.ToolButton
import ch.magori.blox.feature.composer.ui.ToolIcon
import ch.magori.blox.feature.composer.ui.buttonLabelStyle

/** The tabs *Mélodie* and *Basse*: the open [track], in the colours of its tab ([trackColors]). */
@Composable
internal fun TrackTabs(
    track: Track,
    onTrackChosen: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
    ) {
        Track.entries.forEach { tab ->
            val colors = trackColors(palette, tab)
            ChoiceButton(
                text = stringResource(tab.label),
                selected = tab == track,
                onClick = { onTrackChosen(tab) },
                role = Role.Tab,
                selectedColor = colors.tab,
                selectedInk = colors.tabInk,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The toolbar: *Poser* / *Allonger*, the [mode] of the touches, then *Annuler*, *Copier la mesure* and *Effacer la
 * mesure*, each disabled when it has no effect. On one line when each label holds whole there at its smallest size;
 * otherwise the modes above the tools, each label whole and larger, rather than cut (the pixel font, twice as large),
 * and the tools one above the other if they do not hold side by side either ([toolbarLines]).
 */
@Composable
internal fun ComposerToolbar(
    mode: EditMode,
    canUndo: Boolean,
    canCopyBar: Boolean,
    canClearBar: Boolean,
    onModeChosen: (EditMode) -> Unit,
    onUndo: () -> Unit,
    onCopyBar: () -> Unit,
    onClearBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = EditMode.entries.associateWith { stringResource(it.label) }
    val undo = stringResource(R.string.feature_composer_tool_undo)
    val copy = stringResource(R.string.feature_composer_tool_copy)
    val clear = stringResource(R.string.feature_composer_tool_clear)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val style = buttonLabelStyle
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val labels = remember(style, density, modes, undo, copy, clear) {
            // Each label at its smallest size, as SingleLineText shrinks it.
            val smallest = style.copy(fontSize = density.shrinkFloor(style.fontSize))
            val measured = { text: String -> MeasuredLabel(text, measurer.widestWord(text, smallest)) }
            modes.values.map(measured) to listOf(undo, copy, clear).map(measured)
        }
        val lines = with(density) {
            toolbarLines(
                modes = labels.first,
                tools = labels.second,
                width = constraints.maxWidth,
                gap = ComposerSizes.gap.roundToPx(),
                padding = framedBoxPaddingPx(0.dp),
                stackedPadding = framedBoxPaddingPx(StackedLabelPadding),
            )
        }
        val labelPadding = if (lines == ToolbarLines.ONE) 0.dp else StackedLabelPadding
        // Each button as wide as its label needs, with a character to spare: equal widths cut the longest labels of
        // the pixel font on 360 dp, widths of the label alone leave « Poser » under 48 dp.
        val modeGroup: @Composable (Modifier) -> Unit = { groupModifier ->
            ChoiceGroup(
                choices = EditMode.entries,
                selected = mode,
                onChosen = onModeChosen,
                label = { modes.getValue(it) },
                modifier = groupModifier,
                labelPadding = labelPadding,
                choiceModifier = { Modifier.weight(widthOf(modes.getValue(it))) },
                icon = { ToolIcon(it.icon, above = true) },
            )
        }
        val tools: @Composable ((String) -> Modifier) -> Unit = { sizing ->
            ToolButton(
                text = undo,
                onClick = onUndo,
                enabled = canUndo,
                icon = ToolIcon(BloxIcon.UNDO, above = true),
                labelPadding = labelPadding,
                modifier = sizing(undo),
            )
            ToolButton(
                text = copy,
                description = stringResource(R.string.feature_composer_tool_copy_description),
                onClick = onCopyBar,
                enabled = canCopyBar,
                icon = ToolIcon(BloxIcon.COPY, above = true),
                labelPadding = labelPadding,
                modifier = sizing(copy),
            )
            ToolButton(
                text = clear,
                description = stringResource(R.string.feature_composer_tool_clear_description),
                onClick = onClearBar,
                enabled = canClearBar,
                icon = ToolIcon(BloxIcon.ERASE, above = true),
                labelPadding = labelPadding,
                modifier = sizing(clear),
            )
        }
        val gap = Arrangement.spacedBy(ComposerSizes.gap)
        when (lines) {
            ToolbarLines.ONE -> Row(horizontalArrangement = gap) {
                modeGroup(Modifier.weight(modes.values.map(::widthOf).sum()))
                tools { Modifier.weight(widthOf(it)) }
            }

            ToolbarLines.TWO -> Column(verticalArrangement = gap) {
                modeGroup(Modifier.fillMaxWidth())
                Row(horizontalArrangement = gap) { tools { Modifier.weight(widthOf(it)) } }
            }

            ToolbarLines.TOOLS_STACKED -> Column(verticalArrangement = gap) {
                modeGroup(Modifier.fillMaxWidth())
                tools { Modifier.fillMaxWidth() }
            }
        }
    }
}

/** How the toolbar stands: on one line, the modes above the tools, or the modes above each tool. */
internal enum class ToolbarLines { ONE, TWO, TOOLS_STACKED }

/** A label of the toolbar, [text], [width] pixels wide at its smallest size. */
internal class MeasuredLabel(val text: String, val width: Int)

/**
 * How the toolbar stands on a line [width] wide, [gap] between its buttons, all in pixels, given the labels of its
 * [modes] and [tools] at their smallest size, each within its share of its line ([widthOf], [rowShares]) less a padding
 * on each side: [ONE][ToolbarLines.ONE] line if they all hold, with [padding] (to the pixel: with the normal font,
 * « Allonger » in the pixel font fills its share at 8 sp); else [TWO][ToolbarLines.TWO] if the tools hold side by side
 * on the whole width, with [stackedPadding]; else [TOOLS_STACKED][ToolbarLines.TOOLS_STACKED]. The modes, two only,
 * always hold on the whole width when the tools do.
 */
internal fun toolbarLines(
    modes: List<MeasuredLabel>,
    tools: List<MeasuredLabel>,
    width: Int,
    gap: Int,
    padding: Int,
    stackedPadding: Int,
): ToolbarLines {
    val line = rowShares(width, gap, listOf(modes.weights().sum()) + tools.weights())
    val oneLine = rowShares(line.first(), gap, modes.weights()) + line.drop(1)
    return when {
        labelsHold(oneLine, (modes + tools).map { it.width }, padding) -> ToolbarLines.ONE
        holdSideBySide(width, gap, stackedPadding, tools.weights(), tools.map { it.width }) -> ToolbarLines.TWO
        else -> ToolbarLines.TOOLS_STACKED
    }
}

private fun List<MeasuredLabel>.weights(): List<Float> = map { widthOf(it.text) }

/** On each side of a label of the toolbar once it stands on several lines: its labels, larger, do not touch the frame. */
private val StackedLabelPadding = 4.dp

/**
 * The eight bar buttons, the selected [bar], counted from 0, filled; the bar played while the music is listened to,
 * [playedBar], marked apart, and read « en lecture ». They are 48 dp high but narrower than 48 dp on a phone, as the
 * start levels of the home screen: eight of them do not fit otherwise on 360 dp.
 */
@Composable
internal fun BarButtons(
    bar: Int,
    playedBar: Int?,
    onBarChosen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val played = stringResource(R.string.feature_composer_bar_played)
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
    ) {
        (0 until BARS).forEach { index ->
            ChoiceButton(
                text = (index + 1).toString(),
                description = stringResource(R.string.feature_composer_bar, index + 1),
                selected = index == bar,
                onClick = { onBarChosen(index) },
                role = Role.Tab,
                mark = played.takeIf { index == playedBar },
                modifier = Modifier.weight(1f).testTag("composer_bar"),
            )
        }
    }
}

/** The tempo of the music, [bpm] beats per minute, between its buttons « − » and « + », disabled at the ends. */
@Composable
internal fun TempoPanel(
    bpm: Int,
    canSlowDown: Boolean,
    canSpeedUp: Boolean,
    onSlower: () -> Unit,
    onFaster: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(
            text = stringResource(R.string.feature_composer_slower_glyph),
            description = stringResource(R.string.feature_composer_slower),
            onClick = onSlower,
            enabled = canSlowDown,
            modifier = Modifier.width(BloxSizes.minTouchTarget),
        )
        val style = MaterialTheme.typography.labelLarge.shrinkingLineHeight()
        Text(
            text = pluralStringResource(R.plurals.feature_composer_tempo, bpm, bpm),
            color = BloxTheme.palette.ink,
            style = style,
            textAlign = TextAlign.Center,
            // One line if it can, shrunk; else « 120 » above « temps/min », never cut at its slash.
            autoSize = shrinkToFewestLines(style.fontSize),
            // The new tempo is read out after a touch on « − » or « + ». Measured after them, so that a large font
            // never pushes « + » out of the screen.
            modifier = Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite },
        )
        ToolButton(
            text = stringResource(R.string.feature_composer_faster_glyph),
            description = stringResource(R.string.feature_composer_faster),
            onClick = onFaster,
            enabled = canSpeedUp,
            modifier = Modifier.width(BloxSizes.minTouchTarget),
        )
    }
}

@get:StringRes
private val Track.label: Int
    get() = when (this) {
        Track.MELODY -> R.string.feature_composer_tab_melody
        Track.BASS -> R.string.feature_composer_tab_bass
    }

@get:StringRes
private val EditMode.label: Int
    get() = when (this) {
        EditMode.PLACE -> R.string.feature_composer_tool_place
        EditMode.LENGTHEN -> R.string.feature_composer_tool_lengthen
    }

/** The icon of a mode of the touches, above its label. */
private val EditMode.icon: BloxIcon
    get() = when (this) {
        EditMode.PLACE -> BloxIcon.PLACE
        EditMode.LENGTHEN -> BloxIcon.EXTEND
    }

/** The share of the toolbar given to a button labelled [label]: its characters, and one to spare. */
private fun widthOf(label: String): Float = label.length + 1f
