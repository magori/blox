package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.gloss

/** The tag of the track of a [VolumeSlider], read by the tests only. */
internal const val VOLUME_SLIDER_TRACK_TAG = "volume_slider_track"

/** The steps of a volume, in percent: from 0 to 100 by 10. */
internal val VolumeSteps: IntProgression = 0..100 step 10

/** The height of the track of a volume slider, and the side of its knob. */
private val TrackHeight = 6.dp
private val KnobSize = 28.dp

/** The frame of the track of a disabled slider, drawn on its edge: thin, the track being 6 dp high. */
private val TrackFrame = 1.dp

/**
 * A volume slider (docs/design-system/README.md, Paramètres): a track of 6 dp in `panel-border`, filled in `accent`
 * up to a knob of 28 dp in `button`, by steps of 10 % ([VolumeSteps]), its value in percent on its right. TalkBack
 * reads one slider, [description] and the value, and moves it 10 % at a time. Focused by the keyboard, it bears the
 * ring of the buttons, and its arrows move it 10 %, left and right the way they point along the track.
 * [onPercentChange] is given the volume asked for, always one of [VolumeSteps]; [enabled] false, the slider is flat,
 * in `panel-inset`, and cannot be moved nor focused.
 *
 * @throws IllegalArgumentException if [percent] is not a multiple of 10 from 0 to 100.
 */
@Composable
fun VolumeSlider(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val given = SteppedValue(percent, VolumeSteps)
    val gesture = rememberStepGesture()
    val value = gesture.shown(given)
    val text = stringResource(R.string.core_designsystem_volume_percent, value.value)
    val latestValue by rememberUpdatedState(given)
    val change = rememberStepChange(onPercentChange)
    val direction = LocalLayoutDirection.current
    Row(
        modifier = modifier
            .stepSemantics(value, description, state = text, enabled = enabled, onValueChange = change)
            .stepKeys(direction, BloxTheme.buttons, BloxTheme.shapes.md, gesture, change, enabled) {
                latestValue
            },
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val touch = if (enabled) {
            Modifier.stepTouch(KnobSize / 2, direction, {
                latestValue
            }, gesture, change)
        } else {
            Modifier
        }
        Track(value, enabled, Modifier.weight(1f).then(touch))
        ValueText(text, enabled, widest = stringResource(R.string.core_designsystem_volume_percent, VolumeSteps.last))
    }
}

/** The track of a slider at [value], as high as a touch target, its knob at the value. */
@Composable
private fun Track(
    value: SteppedValue,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val colors = if (enabled) stepColors(palette) else disabledStepColors(palette)
    val shape = BloxTheme.shapes.pill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BloxSizes.minTouchTarget)
            .testTag(VOLUME_SLIDER_TRACK_TAG)
            .drawWithCache {
                // The track, and its part from its start to the centre of the knob, as long as the value says.
                val trackHeight = TrackHeight.toPx()
                val reached = KnobSize.toPx() / 2 + (size.width - KnobSize.toPx()) * value.fraction
                val track = shape.createOutline(Size(size.width, trackHeight), layoutDirection, this)
                val fill = shape.createOutline(Size(reached, trackHeight), layoutDirection, this)
                val fillLeft = if (layoutDirection == LayoutDirection.Rtl) size.width - reached else 0f
                val frame = colors.frame
                val frameStroke = Stroke(TrackFrame.toPx())
                onDrawBehind {
                    translate(top = (size.height - trackHeight) / 2) {
                        drawOutline(track, colors.beyond)
                        if (frame != null) drawOutline(track, frame, style = frameStroke)
                        translate(left = fillLeft) { drawOutline(fill, colors.reached) }
                    }
                }
            },
    ) {
        Knob(
            enabled,
            colors.frame,
            Modifier.align(BiasAlignment(horizontalBias = value.fraction * 2 - 1, verticalBias = 0f)),
        )
    }
}

/**
 * The knob of a slider: a bubble of [KnobSize] in `button`, framed by `button-outline`; flat when not [enabled], and
 * framed by [disabledFrame] if there is one.
 */
@Composable
private fun Knob(
    enabled: Boolean,
    disabledFrame: Color?,
    modifier: Modifier = Modifier,
) {
    val buttons = BloxTheme.buttons
    val bubble = if (enabled) buttons.secondary else buttons.disabled
    val shape = BloxTheme.shapes.pill
    val frame = if (enabled) bubble.frame else disabledFrame ?: bubble.frame
    Box(
        modifier = modifier
            .size(KnobSize)
            .background(bubble.fill, shape)
            .gloss(if (enabled) BloxTheme.glosses.button else Gloss.None, shape, BloxBorders.frame)
            .then(if (frame == null) Modifier else Modifier.border(BloxBorders.frame, frame, shape)),
    )
}

/**
 * The value of a slider, [text], at the end of a box as wide as the [widest] value, so that the track keeps its length
 * whatever the value, in `ink`, or in `label` when not [enabled]. A node of its own, left out by TalkBack, which reads
 * the value with the slider.
 */
@Composable
private fun ValueText(
    text: String,
    enabled: Boolean,
    widest: String,
) {
    val style = BloxTheme.typography.choice
    val ink = if (enabled) BloxTheme.palette.ink else BloxTheme.palette.label
    Box(
        modifier = Modifier.semantics(mergeDescendants = true) { hideFromAccessibility() },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(text = widest, style = style, modifier = Modifier.alpha(0f).clearAndSetSemantics {})
        Text(text = text, style = style, color = ink)
    }
}
