package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.gloss
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords

/**
 * A setting of the settings page (docs/design-system/components/Switch), on or off as [checked]: its [icon] in a slot
 * of 40 dp, its [label] and, under it, its [subLine], which says what it does, then its track, on the right. The whole
 * row is the switch, for the finger and for TalkBack, which reads it once: « Vibrations, Lignes effacées, fin de
 * partie, interrupteur, éteint ». Its state is read « allumé » or « éteint », not « activé » or « désactivé », which
 * TalkBack also says of what cannot be touched. Pressed, the row fills with `buttonPressed` and the thumb grows to
 * 28 dp, at once, and both come back in `motion-press`; focused by the keyboard, the row bears a ring of 3 dp in `ink`.
 * With a [disabledReason], the row is disabled, flat and greyed, and its sub-line says that reason instead: TalkBack
 * reads it as unavailable, and why. In a large font, no word is cut, and the label shrinks only for that: when its
 * longest word no longer fits beside the track, the track goes under the label and the sub-line, on the right, the
 * pictogram staying beside them.
 */
@Composable
fun SettingSwitchRow(
    icon: BloxIcon,
    label: String,
    subLine: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    disabledReason: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val motion = rememberPressMotion(interactionSource)
    val focused by interactionSource.collectIsFocusedAsState()
    val depth = remember(motion) { { motion.depth } }
    SettingSwitchRow(
        icon = icon,
        label = label,
        subLine = subLine,
        checked = checked,
        onCheckedChange = onCheckedChange,
        depth = depth,
        focused = focused,
        interactionSource = interactionSource,
        modifier = modifier,
        disabledReason = disabledReason,
    )
}

/**
 * A setting as above, as far pressed as [depth] says and [focused] or not, whatever its [interactionSource] says: the
 * previews draw each state so. Its colours change twice, when it is pressed and when it is back; the thumb is sized
 * where it is placed, so that its way back moves it without composing the row again.
 */
@Composable
internal fun SettingSwitchRow(
    icon: BloxIcon,
    label: String,
    subLine: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    depth: () -> Float,
    focused: Boolean,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier,
    disabledReason: String? = null,
) {
    val enabled = disabledReason == null
    val down by remember(depth) { derivedStateOf { depth() > 0f } }
    val state = SwitchState(checked, enabled, depth = if (down) 1f else 0f, focused = focused)
    val look = switchLook(BloxTheme.variant, state)
    val shape = BloxTheme.shapes.md
    val stateText =
        stringResource(if (checked) R.string.core_designsystem_switch_on else R.string.core_designsystem_switch_off)
    SettingRowLayout(
        modifier = modifier
            .focusRing(look.focusRing, shape)
            .fillMaxWidth()
            .heightIn(min = BloxSizes.settingRow)
            .background(look.rowFill, shape)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { stateDescription = stateText }
            .padding(BloxSpaces.space2),
        labels = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(ICON_SLOT), contentAlignment = Alignment.Center) {
                    BloxIcon(icon, size = IconSizes.alone, tint = look.subLine)
                }
                Column(
                    modifier = Modifier.padding(start = BloxSpaces.space3).weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val labelStyle = labelStyle()
                    Text(
                        text = label,
                        color = look.label,
                        style = labelStyle,
                        maxLines = 2,
                        autoSize = shrinkToWholeWords(labelStyle.fontSize),
                    )
                    val subLineStyle = MaterialTheme.typography.bodyMedium.merge(BloxTheme.typography.small)
                    Text(
                        text = disabledReason ?: subLine,
                        color = look.subLine,
                        style = subLineStyle,
                        maxLines = 2,
                        autoSize = shrinkToWholeWords(subLineStyle.fontSize),
                    )
                }
            }
        },
        track = { SwitchTrack(look) { thumbSize(checked, enabled, depth()) } },
    )
}

/**
 * The [labels] of a setting (its pictogram, its label and its sub-line) and its [track]: on one line, the track on the
 * right, while the longest word of the labels fits beside it; else the labels take the whole line and the track goes
 * under them, on the right. Each line is centred in the height of the row.
 */
@Composable
private fun SettingRowLayout(
    labels: @Composable () -> Unit,
    track: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(labels, track), modifier = modifier) {
            (labelsMeasurables, trackMeasurables),
            constraints,
        ->
        val width = constraints.maxWidth
        val gap = BloxSpaces.space3.roundToPx()
        val trackPlaceable = trackMeasurables.single().measure(Constraints())
        val labelsMeasurable = labelsMeasurables.single()
        val besideRoom = width - gap - trackPlaceable.width
        val beside = labelsMeasurable.minIntrinsicWidth(Constraints.Infinity) <= besideRoom
        val labelsPlaceable = labelsMeasurable.measure(Constraints(maxWidth = if (beside) besideRoom else width))
        val content = if (beside) {
            maxOf(labelsPlaceable.height, trackPlaceable.height)
        } else {
            labelsPlaceable.height + BloxSpaces.space1.roundToPx() + trackPlaceable.height
        }
        val height = maxOf(content, constraints.minHeight)
        layout(width, height) {
            val top = (height - content) / 2
            if (beside) {
                labelsPlaceable.place(0, top + (content - labelsPlaceable.height) / 2)
                trackPlaceable.place(width - trackPlaceable.width, top + (content - trackPlaceable.height) / 2)
            } else {
                labelsPlaceable.place(0, top)
                trackPlaceable.place(width - trackPlaceable.width, top + content - trackPlaceable.height)
            }
        }
    }
}

/** The track of a switch drawn as [look] says, its thumb as large as [thumbSize] says when it is placed. */
@Composable
private fun SwitchTrack(
    look: SwitchLook,
    thumbSize: () -> DpSize,
) {
    val shape = BloxTheme.shapes.pill
    Box(
        modifier = Modifier
            .testTag(SWITCH_TRACK_TAG)
            .size(BloxSizes.switchTrack, TRACK_HEIGHT)
            .background(look.track, shape)
            .gloss(look.trackGloss, shape, BloxBorders.frame)
            .border(BloxBorders.frame, look.trackFrame, shape)
            .padding(BloxBorders.frame + THUMB_GAP),
        contentAlignment = if (look.thumbAtEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                // Off, the thumb stands as far from the side of the track as from its top and its bottom.
                .padding(start = if (look.thumbAtEnd) 0.dp else THUMB_GAP)
                .testTag(SWITCH_THUMB_TAG)
                .layout { measurable, _ ->
                    val size = thumbSize()
                    val width = size.width.roundToPx()
                    val height = size.height.roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .thumbFill(look, shape),
            contentAlignment = Alignment.Center,
        ) {
            look.check?.let { ThumbCheck(it, Modifier.size(CHECK_SIDE)) }
        }
    }
}

/** The thumb of [look] in [shape]: full, or its outline alone when it is hollow. */
private fun Modifier.thumbFill(
    look: SwitchLook,
    shape: Shape,
): Modifier = if (look.thumbHollow) border(HOLLOW_THUMB_OUTLINE, look.thumb, shape) else background(look.thumb, shape)

/**
 * The check of a thumb, in [color]: the `CHECK` pictogram of the design system (docs/design-system/pictogrammes.md),
 * a stroke of 3 on a grid of 24, rounded. Decorative: the row already says its state.
 */
@Composable
private fun ThumbCheck(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val unit = size.minDimension / CHECK_GRID
        val path = Path().apply {
            moveTo(4 * unit, 12.5f * unit)
            lineTo(9 * unit, 17.5f * unit)
            lineTo(20 * unit, 6.5f * unit)
        }
        drawPath(path, color, style = Stroke(CHECK_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * The style of the label of a setting: `choice` in bold (Manrope 15/700), over the Material label, whose size Original
 * keeps; in Original, its pixel font has a single weight, which a synthesised bold would blur.
 */
@Composable
@ReadOnlyComposable
private fun labelStyle(): TextStyle {
    val choice = BloxTheme.typography.choice
    val style = if (BloxTheme.kind == BloxThemeKind.ORIGINAL) choice else choice.copy(fontWeight = FontWeight.Bold)
    return MaterialTheme.typography.labelLarge.merge(style)
}

/** The tags of the track and of the thumb of a switch, read by the tests. */
internal const val SWITCH_TRACK_TAG = "switch_track"
internal const val SWITCH_THUMB_TAG = "switch_thumb"

/** The slot of the pictogram of a setting. */
private val ICON_SLOT = 40.dp

/** The height of the track of a switch (`switch-track`: 52 × 32). */
private val TRACK_HEIGHT = 32.dp

/** Between the frame of a track and its thumb. */
private val THUMB_GAP = 2.dp

/** The check of a thumb on, and its grid and stroke (pictogram `CHECK`). */
private val CHECK_SIDE = 14.dp
private const val CHECK_GRID = 24f
private const val CHECK_STROKE = 3f
