package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonEmphasis
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.ButtonSize
import ch.magori.blox.core.designsystem.component.ChoiceColors
import ch.magori.blox.core.designsystem.component.ChoiceItem
import ch.magori.blox.core.designsystem.component.ChoiceSegments
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.lineWidth
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkFloor
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines
import ch.magori.blox.core.designsystem.theme.shrinkingLineHeight
import ch.magori.blox.feature.composer.grid.trackColors
import ch.magori.blox.feature.composer.model.BARS
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.ui.ComposerSizes

/**
 * The tracks *Mélodie* and *Basse*, segments read as the radio buttons of « Piste »: the open [track] filled in the
 * colours of its tab ([trackColors]) and checked.
 */
@Composable
internal fun TrackTabs(
    track: Track,
    onTrackChosen: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val choices = Track.entries.map { tab ->
        val colors = trackColors(palette, tab)
        ChoiceItem(stringResource(tab.label), selectedColors = ChoiceColors(colors.tab, colors.tabInk))
    }
    ChoiceSegments(
        name = stringResource(R.string.feature_composer_tracks),
        choices = choices,
        selected = track.ordinal,
        onChosen = { onTrackChosen(Track.entries[it]) },
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * *Poser* / *Allonger*, the [mode] of the touches, segments read as the radio buttons of « Outil », each after its
 * pictogram, the retained one checked; then *Annuler*, disabled when there is nothing to undo ([canUndo]), on the rest
 * of the line, or under them when the line is too narrow for both ([FixedBesideRest]).
 */
@Composable
internal fun ModesAndUndo(
    mode: EditMode,
    canUndo: Boolean,
    onModeChosen: (EditMode) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FixedBesideRest(fixedFirst = true, restMin = BloxSizes.minTouchTarget, modifier = modifier) {
        ChoiceSegments(
            name = stringResource(R.string.feature_composer_tools),
            choices = EditMode.entries.map { ChoiceItem(stringResource(it.label), icon = it.icon) },
            selected = mode.ordinal,
            onChosen = { onModeChosen(EditMode.entries[it]) },
        )
        ToolTile(
            text = stringResource(R.string.feature_composer_tool_undo),
            icon = BloxIcon.UNDO,
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The eight bar buttons, on two rows of four, the selected [bar], counted from 0, in `accent`; then *Copier la mesure*
 * and *Effacer la mesure*, one above the other, each disabled when it has no effect ([canCopyBar], [canClearBar]), or
 * under the bars, side by side, when the line is too narrow for the bars beside them ([FixedBesideRest]).
 */
@Composable
internal fun BarsAndTools(
    bar: Int,
    playedBar: Int?,
    canCopyBar: Boolean,
    canClearBar: Boolean,
    onBarChosen: (Int) -> Unit,
    onCopyBar: () -> Unit,
    onClearBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barsMin = BloxSizes.minTouchTarget * BARS_PER_ROW + ComposerSizes.gap * (BARS_PER_ROW - 1)
    FixedBesideRest(fixedFirst = false, restMin = barsMin, modifier = modifier) {
        BarButtons(bar = bar, playedBar = playedBar, onBarChosen = onBarChosen)
        ColumnOrRow {
            ToolTile(
                text = stringResource(R.string.feature_composer_tool_copy),
                description = stringResource(R.string.feature_composer_tool_copy_description),
                icon = BloxIcon.COPY,
                onClick = onCopyBar,
                enabled = canCopyBar,
            )
            ToolTile(
                text = stringResource(R.string.feature_composer_tool_clear),
                description = stringResource(R.string.feature_composer_tool_clear_description),
                icon = BloxIcon.ERASE,
                onClick = onClearBar,
                enabled = canClearBar,
            )
        }
    }
}

/**
 * The eight bar buttons, on two rows of four, read as radio buttons, the selected [bar], counted from 0, in `accent`;
 * the bar played while the music is listened to, [playedBar], read « en lecture », or « sélectionnée, en lecture » when it
 * is the selected one too: a state given replaces the selection TalkBack would have said.
 */
@Composable
private fun BarButtons(
    bar: Int,
    playedBar: Int?,
    onBarChosen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val played = stringResource(R.string.feature_composer_bar_played)
    val playedAndChosen = stringResource(R.string.feature_composer_bar_selected_played)
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
    ) {
        (0 until BARS).chunked(BARS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap)) {
                row.forEach { index ->
                    val description = stringResource(R.string.feature_composer_bar, index + 1)
                    val chosen = index == bar
                    val buttons = BloxTheme.buttons
                    val ink = if (chosen) buttons.selected.ink else buttons.secondary.ink
                    BubbleButton(
                        family = ButtonFamily.SECONDARY,
                        onClick = { onBarChosen(index) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(BAR_TAG)
                            .semantics { contentDescription = description }
                            .marked(
                                (if (chosen) playedAndChosen else played).takeIf { index == playedBar },
                                ink,
                            ),
                        shape = BloxTheme.shapes.md,
                        emphasis = if (chosen) ButtonEmphasis.SELECTED else ButtonEmphasis.PLAIN,
                        size = ButtonSize.COMPACT,
                        sidePadding = 0.dp,
                        role = Role.RadioButton,
                    ) {
                        Text(text = (index + 1).toString(), maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * The line of a [mark], if any, under the number of the bar it marks, in [ink], the colour of its number; TalkBack
 * reads the mark as the state of the bar.
 */
private fun Modifier.marked(
    mark: String?,
    ink: Color,
): Modifier = if (mark == null) {
    this
} else {
    semantics { stateDescription = mark }.drawWithContent {
        drawContent()
        val width = MarkWidth.toPx()
        val height = MarkHeight.toPx()
        drawRoundRect(
            color = ink,
            topLeft = Offset((size.width - width) / 2, size.height - height - MarkInset.toPx()),
            size = Size(width, height),
            cornerRadius = CornerRadius(height / 2),
        )
    }
}

/**
 * A tool of the editor: a compact tile of the family Secondaire, its [icon], decorative, then [text]; TalkBack reads
 * [description] in its place when the text alone is too short to say what it does. Disabled when it has no effect.
 */
@Composable
private fun ToolTile(
    text: String,
    icon: BloxIcon,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    BubbleButton(
        family = ButtonFamily.SECONDARY,
        onClick = onClick,
        modifier = if (description == null) modifier else modifier.semantics { contentDescription = description },
        enabled = enabled,
        shape = BloxTheme.shapes.md,
        size = ButtonSize.COMPACT,
        sidePadding = BloxSpaces.space3,
    ) {
        BloxIcon(icon)
        Text(text = text)
    }
}

/**
 * Two parts on one line, [ComposerSizes.gap] apart, the first at the start, the second at the end: the fixed one, first
 * when [fixedFirst], as wide as it asks, and up to the rest of the line to the other, while that rest is [restMin] wide
 * at least and holds what it shows at its size; otherwise one above the other, in their order, each up to the width of
 * the line, at its end, rather than cut a word of a font made larger. A part that fills its room says so itself
 * (`fillMaxWidth`).
 */
@Composable
internal fun FixedBesideRest(
    fixedFirst: Boolean,
    restMin: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        require(measurables.size == 2) { "Two parts stand beside each other, not ${measurables.size}." }
        val (first, second) = measurables
        val fixed = if (fixedFirst) first else second
        val rest = if (fixedFirst) second else first
        val width = constraints.maxWidth
        val gap = ComposerSizes.gap.roundToPx()
        val fixedWidth = fixed.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(width)
        val restWidth = width - fixedWidth - gap
        val restNeeds = maxOf(restMin.roundToPx(), rest.minIntrinsicWidth(Constraints.Infinity))
        if (fitsBesideFixed(width, gap, fixedWidth, restNeeds)) {
            val fixedPlaced = fixed.measure(Constraints(maxWidth = fixedWidth))
            val restPlaced = rest.measure(Constraints(maxWidth = restWidth))
            val (start, end) = if (fixedFirst) fixedPlaced to restPlaced else restPlaced to fixedPlaced
            val height = maxOf(start.height, end.height)
            layout(width, height) {
                start.placeRelative(0, Alignment.CenterVertically.align(start.height, height))
                end.placeRelative(width - end.width, Alignment.CenterVertically.align(end.height, height))
            }
        } else {
            val placed = listOf(first, second).map { it.measure(Constraints(maxWidth = width)) }
            layout(width, placed.sumOf { it.height } + gap) {
                var y = 0
                placed.forEach {
                    it.placeRelative(width - it.width, y)
                    y += it.height + gap
                }
            }
        }
    }
}

/**
 * Its parts one above the other, as wide as the widest asks, or as the line when they do not hold side by side; side by
 * side, as wide as each other, once given a line wide enough for them all: the tools beside the bars, or under them.
 */
@Composable
private fun ColumnOrRow(content: @Composable () -> Unit) {
    val gap = with(LocalDensity.current) { ComposerSizes.gap.roundToPx() }
    val policy = remember(gap) { ColumnOrRowPolicy(gap) }
    Layout(content = content, measurePolicy = policy)
}

/** How [ColumnOrRow] lays its parts out, [gap] pixels apart, as wide as it is given, or as the widest asks. */
private data class ColumnOrRowPolicy(private val gap: Int) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val widest = measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else widest
        val share = (width - gap * (measurables.size - 1)) / measurables.size
        return if (sideBySide(width, gap, measurables.size, widest)) {
            val placed = measurables.map { it.measure(Constraints.fixedWidth(share)) }
            layout(width, placed.maxOf { it.height }) {
                placed.forEachIndexed { index, it -> it.placeRelative(index * (share + gap), 0) }
            }
        } else {
            val placed = measurables.map { it.measure(Constraints.fixedWidth(width)) }
            layout(placed.maxOf { it.width }, placed.sumOf { it.height } + gap * (placed.size - 1)) {
                var y = 0
                placed.forEach {
                    it.placeRelative(0, y)
                    y += it.height + gap
                }
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOf { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOf { it.minIntrinsicWidth(height) }
}

/**
 * The tempo of the music, [bpm] beats per minute, between its bare discs « − » and « + », disabled at the ends, in a
 * pill filled and framed as a secondary button.
 */
@Composable
internal fun TempoPanel(
    bpm: Int,
    canSlowDown: Boolean,
    canSpeedUp: Boolean,
    onSlower: () -> Unit,
    onFaster: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = BloxTheme.shapes.pill
    val colors = BloxTheme.buttons.secondary
    Row(
        modifier = modifier
            .background(colors.fill, pill)
            .border(BloxBorders.frame, colors.frame ?: colors.fill, pill)
            .padding(BloxBorders.frame),
        horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_composer_slower),
            onClick = onSlower,
            enabled = canSlowDown,
            emphasis = ButtonEmphasis.BARE,
        ) { Glyphs.minus(this, it) }
        val style = MaterialTheme.typography.labelLarge.shrinkingLineHeight()
        val text = pluralStringResource(R.plurals.feature_composer_tempo, bpm, bpm)
        // On one line at its smallest size, the least room of the tempo, which the line of the tempo and Écouter counts
        // before standing side by side: on two lines, « 120 temps/ » would go before « min », cut at its slash.
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val oneLine = remember(text, style, density) {
            with(density) {
                measurer.lineWidth(text, style.copy(fontSize = shrinkFloor(style.fontSize))).toDp()
            }
        }
        Text(
            text = text,
            color = BloxTheme.palette.ink,
            style = style,
            textAlign = TextAlign.Center,
            // One line if it can, shrunk; else « 120 » above « temps/min », never cut at its slash.
            autoSize = shrinkToFewestLines(style.fontSize),
            // The new tempo is read out after a touch on « − » or « + ». Measured after them, so that a large font
            // never pushes « + » out of the screen.
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(min = maxOf(TempoMinWidth, oneLine))
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        GlyphButton(
            description = stringResource(R.string.feature_composer_faster),
            onClick = onFaster,
            enabled = canSpeedUp,
            emphasis = ButtonEmphasis.BARE,
        ) { Glyphs.plus(this, it) }
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

/** The icon of a mode of the touches, before its label. */
private val EditMode.icon: BloxIcon
    get() = when (this) {
        EditMode.PLACE -> BloxIcon.PLACE
        EditMode.LENGTHEN -> BloxIcon.EXTEND
    }

/** The tempo between « − » and « + »: as wide as « 200 », at least, so that the pill does not move as it changes. */
private val TempoMinWidth = 64.dp

/** The line under the number of the bar played: this wide, this high, this far above the bottom edge of the bar. */
private val MarkWidth = 20.dp
private val MarkHeight = 3.dp
private val MarkInset = 6.dp

/** The bar buttons stand on rows of four: two rows for the eight bars. */
private const val BARS_PER_ROW = 4

/** The tag of a bar button, read by the tests. */
internal const val BAR_TAG = "composer_bar"
