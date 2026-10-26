package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/**
 * A choice of a [ChoiceGroup]: its [label], on one line or up to [maxLines], shrinking if need be, read by TalkBack as
 * [description] instead, if given. A choice that is not [available] bears « BIENTÔT » and cannot be chosen.
 */
@Immutable
data class Choice(
    val label: String,
    val available: Boolean = true,
    val description: String? = null,
    val maxLines: Int = 1,
)

/**
 * A group of [choices] of which one, [selected], is retained (docs/design-system/components/Choice): the segments
 * stand in one box framed by `button-outline`, as a secondary button, which makes them seen rather than a frame of each
 * one; the retained one is filled in `accent` and bears a check, the cue of its state other than colour (P5.57). A
 * touch on a segment that is available tells its index to [onChosen]. TalkBack reads [label] as a heading, then the
 * segments as one group of radio buttons.
 *
 * The group stands on the line of its [label], at its end, when both fit; else its label goes above it, and it takes
 * the whole width, as in a large font. [action], if any, stands beside the label. The segments stand on rows of
 * [columns], in cells as wide as one another, at least [BloxSizes.minTouchTarget] high: on one row, a pill, each check
 * before its label; on several, a box of `radius-md`, each check in the top corner of its cell, so that a narrow cell
 * keeps its label in its middle.
 */
@Composable
fun ChoiceGroup(
    label: String,
    choices: List<Choice>,
    selected: Int,
    onChosen: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = choices.size,
    action: (@Composable () -> Unit)? = null,
) {
    Layout(
        contents = listOf(
            { GroupLabel(label, action) },
            { Segments(choices, selected, onChosen, columns) },
        ),
        modifier = modifier,
    ) { (labels, groups), constraints ->
        val labelPart = labels.single()
        val group = groups.single()
        val gap = BloxSpaces.space3.roundToPx()
        val labelWidth = labelPart.maxIntrinsicWidth(Constraints.Infinity)
        val groupWidth = group.maxIntrinsicWidth(Constraints.Infinity)
        val wanted = labelWidth + gap + groupWidth
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else wanted
        if (wanted <= width) {
            // Beside: the label at the start of the line, the group at its end, both centred on the line.
            val placedGroup = group.measure(Constraints.fixedWidth(groupWidth))
            val placedLabel = labelPart.measure(Constraints(maxWidth = width - groupWidth - gap))
            val height = maxOf(placedGroup.height, placedLabel.height)
            layout(width, height) {
                placedLabel.place(0, (height - placedLabel.height) / 2)
                placedGroup.place(width - placedGroup.width, (height - placedGroup.height) / 2)
            }
        } else {
            // Above: the label on its own line, the group under it, as wide as the line.
            val placedLabel = labelPart.measure(Constraints(maxWidth = width))
            val placedGroup = group.measure(Constraints.fixedWidth(width))
            val spacing = BloxSpaces.space1.roundToPx()
            layout(width, placedLabel.height + spacing + placedGroup.height) {
                placedLabel.place(0, 0)
                placedGroup.place(0, placedLabel.height + spacing)
            }
        }
    }
}

/** The label of a group, a heading, and its [action] beside it, or under it when short of room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupLabel(
    label: String,
    action: (@Composable () -> Unit)?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.section),
            modifier = Modifier.semantics { heading() },
        )
        action?.invoke()
    }
}

/**
 * The box of the segments, filled and framed as a secondary button, its [choices] on rows of [columns], or of fewer
 * when the line is too narrow for so many, down to one under the other: each choice keeps the room of its label, as
 * in a large font.
 */
@Composable
private fun Segments(
    choices: List<Choice>,
    selected: Int,
    onChosen: (index: Int) -> Unit,
    columns: Int,
) {
    val look = groupLook(BloxTheme.buttons)
    val shape = GroupShape(single = BloxTheme.shapes.pill, several = BloxTheme.shapes.md)
    // A grid of narrow cells, as the start levels, bears its checks in their corners.
    val checkInCorner = columns < choices.size
    Layout(
        content = {
            choices.forEachIndexed { index, choice ->
                Segment(
                    choice = choice,
                    selected = index == selected,
                    onClick = { onChosen(index) },
                    checkInCorner = checkInCorner,
                )
            }
        },
        modifier = Modifier
            .background(look.fill, shape)
            .border(BloxBorders.frame, look.frame, shape)
            .selectableGroup(),
        measurePolicy = SegmentsMeasurePolicy(columns),
    )
}

/**
 * Lays segments out on rows of [columns] cells as wide as one another, or of fewer when the width cannot hold so many:
 * a label may shrink to [SHRINK_BEFORE_STACKING] of its size, « BIENTÔT » go under it, before its cells stack. The last
 * row holds those left, under the first cells of the row above. A row is as high as its highest segment, the others
 * centred on it.
 */
private class SegmentsMeasurePolicy(private val columns: Int) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val widest = measurables.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else widest * columns
        val widestAtLeast = measurables.maxOfOrNull { it.minIntrinsicWidth(Constraints.Infinity) } ?: 0
        val fitting = (columns downTo 1).firstOrNull { it * widestAtLeast * SHRINK_BEFORE_STACKING <= width } ?: 1
        val cell = width / fitting
        val rows = choiceRows(measurables.size, fitting).map { row ->
            row.map { measurables[it].measure(Constraints(minWidth = cell, maxWidth = cell)) }
        }
        val height = rows.sumOf { row -> row.maxOf { it.height } }
        return layout(width, height) {
            var y = 0
            rows.forEach { row ->
                val rowHeight = row.maxOf { it.height }
                row.forEachIndexed { column, placeable ->
                    placeable.place(column * cell, y + (rowHeight - placeable.height) / 2)
                }
                y += rowHeight
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = (measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0) * columns
}

/**
 * The shape of a group: [single] on one row, a pill, [several] on more, whose corners a pill would cut too deep. A
 * group of one row is no higher than [ONE_ROW].
 */
private class GroupShape(private val single: Shape, private val several: Shape) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val oneRow = with(density) { ONE_ROW.toPx() }
        return (if (size.height <= oneRow) single else several).createOutline(size, layoutDirection, density)
    }
}

/**
 * A segment, as wide as the others of its row and [BloxSizes.minTouchTarget] high at least, frame of the group
 * included: the whole cell is its target. Its fill, when [selected], stands [SEGMENT_INSET] inside it, clear of the
 * frame. Its check stands before its label, or in its top corner if [checkInCorner].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Segment(
    choice: Choice,
    selected: Boolean,
    onClick: () -> Unit,
    checkInCorner: Boolean,
) {
    val look = segmentLook(BloxTheme.buttons, selected, choice.available)
    val description = if (choice.available) {
        choice.description
    } else {
        stringResource(R.string.core_designsystem_choice_unavailable, choice.label)
    }
    val fill = look.fill
    Box(
        modifier = Modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .selectable(
                selected = selected,
                enabled = choice.available,
                role = Role.RadioButton,
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(SEGMENT_INSET)
            .then(if (fill != null) Modifier.background(fill, BloxTheme.shapes.pill) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val style = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.choice)
        // With a description, TalkBack reads it alone, not the label after it.
        val read = description == null
        if (checkInCorner) {
            // The check in the top corner, beside the label rather than before it: the label stays in the middle of
            // its cell, under the labels of the other rows. Every cell keeps that room on both sides of its label,
            // selected or not, so that the check never touches it, even in a large font.
            val room = CORNER_CHECK_INSET + CORNER_CHECK_SIZE
            SegmentLabel(choice, look.ink, style, read, Modifier.padding(horizontal = room))
            look.check?.let {
                CheckMark(
                    color = it,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CORNER_CHECK_INSET)
                        .testTag(CHOICE_CHECK_TAG)
                        .size(CORNER_CHECK_SIZE),
                )
            }
        } else {
            // Before the label, as large as its letters up to a limit, so that it grows with the font.
            val labelSize = with(LocalDensity.current) { style.fontSize.toDp() }
            val insets = choiceInsets(selected = selected, label = labelSize)
            Row(
                modifier = Modifier.padding(start = insets.start, end = insets.end),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                look.check?.let {
                    CheckMark(it, Modifier.testTag(CHOICE_CHECK_TAG).size(insets.check))
                    Spacer(Modifier.width(insets.gap))
                }
                // « BIENTÔT » after the name, or under it when short of room: the name keeps its room.
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(SOON_GAP, Alignment.CenterHorizontally),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    SegmentLabel(choice, look.ink, style, read)
                    if (!choice.available) Soon(look.ink)
                }
            }
        }
    }
}

/**
 * The label of [choice] in [color], on one line or up to its lines, shrinking if need be: in a large font, on a narrow
 * phone, a label may be wider than its segment. TalkBack reads it if [read].
 */
@Composable
private fun SegmentLabel(
    choice: Choice,
    color: Color,
    style: TextStyle,
    read: Boolean,
    modifier: Modifier = Modifier,
) {
    val lines = choice.maxLines
    Text(
        text = choice.label,
        modifier = if (read) modifier else modifier.clearAndSetSemantics { testTag = CHOICE_LABEL_TAG },
        color = color,
        style = style,
        textAlign = if (lines > 1) TextAlign.Center else null,
        overflow = if (lines > 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        maxLines = lines,
        softWrap = lines > 1,
        autoSize = shrinkToFit(style.fontSize),
    )
}

/** « BIENTÔT », in capitals, after the label of a choice not yet available; TalkBack reads its description instead. */
@Composable
private fun Soon(color: Color) {
    Text(
        text = stringResource(R.string.core_designsystem_choice_soon).uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel),
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.clearAndSetSemantics { testTag = CHOICE_SOON_TAG },
    )
}

/** The indices of [count] choices in rows of [columns], in order: the last row holds those left, fewer if need be. */
internal fun choiceRows(
    count: Int,
    columns: Int,
): List<List<Int>> = (0 until count).chunked(columns)

/** The tag of « BIENTÔT », read by the tests. */
internal const val CHOICE_SOON_TAG = "choice_soon"

/** The tag of the label of a choice that TalkBack reads by its description, read by the tests. */
internal const val CHOICE_LABEL_TAG = "choice_label"

/** Between the edge of a segment and its fill: the frame of the group, and as much again. */
private val SEGMENT_INSET = BloxBorders.frame * 2

/** The check in the top corner of a cell: small, so that it fits beside a digit. */
private val CORNER_CHECK_SIZE = 10.dp

/** Between the check in the corner of a cell and the edges of its fill. */
private val CORNER_CHECK_INSET = 4.dp

/** Between the label of a choice not yet available and « BIENTÔT ». */
private val SOON_GAP = 6.dp

/** The highest a group of one row may be, even in a large font: half a row more than the least. */
private val ONE_ROW = BloxSizes.minTouchTarget * 1.5f

/** How small a label may shrink, as a part of its size, before the cells of its group stack: three quarters. */
private const val SHRINK_BEFORE_STACKING = 0.75f
