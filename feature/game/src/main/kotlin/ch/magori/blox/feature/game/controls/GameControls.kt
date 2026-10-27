package ch.magori.blox.feature.game.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.R
import ch.magori.blox.feature.game.ScreenMargin

/**
 * The fingers on the touch controls of V2 and V3, shared by the two groups, [DirectionPad] (◀ ▼ ▶)
 * and [RotationPad] (B A), wherever the layout puts them, and the keys of a keyboard or a pad. No logic
 * here: the fingers and the keys are followed by
 * [HeldButtons]; this only reports when a button goes down or comes back up ([onChange]), so that a
 * held button can repeat (DAS) in the engine. The buttons still held are released when it leaves.
 */
@Composable
internal fun rememberHeldControls(onChange: (ControlChange) -> Unit): HeldControls {
    val currentOnChange by rememberUpdatedState(onChange)
    val controls = remember { HeldControls { change -> currentOnChange(change) } }
    DisposableEffect(controls) {
        onDispose { controls.cancel() }
    }
    return controls
}

/**
 * Applies the events of the fingers and of the keys to [HeldButtons], reports the changes, and shows the
 * held buttons.
 */
@Stable
internal class HeldControls(private val onChange: (ControlChange) -> Unit) {
    private var held = HeldButtons()

    /** The buttons under a finger or held by a key, read by the composition to draw them pressed. */
    var heldButtons: Set<ControlButton> by mutableStateOf(emptySet())
        private set

    fun press(
        finger: Long,
        button: ControlButton,
    ) = report(held.press(finger, button))

    fun lift(finger: Long) = report(held.lift(finger))

    fun tap(button: ControlButton) = report(held.tap(button))

    /**
     * A key event of the game under way, [key] going down or up ([type]) after [repeatCount] repeats:
     * what [keyAction] makes of it, the pause given to [onPause]. True when the key is of the game, so
     * that the system does not act on it too; false for another key, left to the system.
     */
    fun key(
        key: Key,
        type: KeyEventType,
        repeatCount: Int,
        source: KeySource,
        onPause: () -> Unit,
    ): Boolean {
        when (val action = keyAction(key, type, repeatCount, source)) {
            null -> return false
            is KeyAction.Press -> report(held.pressKey(key, action.button))
            KeyAction.Release -> report(held.liftKey(key))
            KeyAction.Pause -> onPause()
            KeyAction.Ignored -> Unit
        }
        return true
    }

    fun cancel() = report(held.cancel())

    private fun report(step: HeldButtons.Step) {
        held = step.held
        heldButtons = step.held.heldButtons
        step.changes.forEach(onChange)
    }
}

/**
 * ◀ ▼ ▶, squares of [keys], [spacing] apart, ▼ lower by [KeySizes.arrowRaise]: [KeySizes.directionsWidth] ×
 * [KeySizes.directionsHeight].
 */
@Composable
internal fun DirectionPad(
    controls: HeldControls,
    modifier: Modifier = Modifier,
    keys: KeySizes = KeySizes.Phone,
    spacing: Dp = ControlSizes.spacing,
) {
    val square = BloxTheme.shapes.md
    ControlGroup(spacing = spacing, modifier = modifier) {
        ControlKey(ControlButton.LEFT, controls, keys.arrow, square)
        ControlKey(ControlButton.DOWN, controls, keys.arrow, square, Modifier.padding(top = keys.arrowRaise))
        ControlKey(ControlButton.RIGHT, controls, keys.arrow, square)
    }
}

/**
 * B and A, discs of [keys], B lower by [ControlSizes.rotationStagger], [ControlSizes.rotationSpacing] apart:
 * [KeySizes.rotationsWidth] × [KeySizes.rotationsHeight]. For the [leftHanded], as in a mirror, A on the outside, on the
 * left; TalkBack still reads B, then A. The icon of each key is drawn at the angle [keyAngle] gives, in degrees, read only
 * while drawing (ADR 0024): it turns as the piece turns without recomposing anything.
 */
@Composable
internal fun RotationPad(
    controls: HeldControls,
    modifier: Modifier = Modifier,
    keys: KeySizes = KeySizes.Phone,
    leftHanded: Boolean = false,
    keyAngle: (ControlButton) -> Float = NoKeyAngle,
) {
    val disc = BloxTheme.shapes.pill
    val b = @Composable {
        ControlKey(
            ControlButton.B,
            controls,
            keys.rotation,
            disc,
            Modifier.padding(top = ControlSizes.rotationStagger),
            readingOrder = 1f,
            glyphAngle = { keyAngle(ControlButton.B) },
        )
    }
    val a = @Composable {
        ControlKey(
            ControlButton.A,
            controls,
            keys.rotation,
            disc,
            readingOrder = 2f,
            glyphAngle = { keyAngle(ControlButton.A) },
        )
    }
    ControlGroup(spacing = ControlSizes.rotationSpacing, modifier = modifier) {
        if (leftHanded) {
            a()
            b()
        } else {
            b()
            a()
        }
    }
}

/**
 * The keys of a group in a row, [spacing] apart: [ControlSizes.spacing], the arrows closer in a window too narrow.
 * The group is excluded from the system gestures (API 29 and above): in gesture navigation, the zone
 * of the Back gesture can be wider than the margins of the screen, and a thumb sliding from ◀ to ▼
 * would otherwise go back, which cancels the fingers and pauses the game. The system honours 200 dp
 * of exclusion per edge, which the groups fit: ◀ ▼ ▶ is [KeySizes.directionsHeight] high, B A
 * [KeySizes.rotationsHeight], and both together would still fit on one edge.
 */
@Composable
private fun ControlGroup(
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.systemGestureExclusion(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = { content() },
    )
}

/** Both groups on one row, as on a phone, for the previews: for the left hand when [leftHanded]. */
@Composable
private fun ControlPad(
    controls: HeldControls,
    modifier: Modifier = Modifier,
    keys: KeySizes = KeySizes.Phone,
    leftHanded: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        if (leftHanded) {
            RotationPad(controls, keys = keys, leftHanded = true)
            DirectionPad(controls, keys = keys)
        } else {
            DirectionPad(controls, keys = keys)
            RotationPad(controls, keys = keys)
        }
    }
}

/**
 * The tag of a key (`control_left`, `control_a`…), the same in every language: the measured journey of
 * `:benchmark` finds the keys by it (`By.res`, with `testTagsAsResourceId` set by `app`).
 */
internal fun controlTestTag(button: ControlButton): String = "control_${button.name.lowercase()}"

/**
 * The key of [button], [size] wide and high, in [shape], read by TalkBack by its name, at [readingOrder] in its group
 * when it has one (B before A, wherever they stand); its glyph drawn at the angle [glyphAngle] gives, read while drawing.
 */
@Composable
private fun ControlKey(
    button: ControlButton,
    controls: HeldControls,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    readingOrder: Float? = null,
    glyphAngle: () -> Float = NoGlyphAngle,
) {
    val held = button in controls.heldButtons
    val (background, outline, foreground) = keyColors(BloxTheme.palette, button, held)
    val description = stringResource(descriptionOf(button))
    Box(
        // Held, the key goes down, whatever the effects: its drop is no animation, and the only cue of A held in the
        // day. Its tag and what TalkBack reads come after the drop, so that they follow the key where it is drawn.
        modifier = modifier
            .offset(y = keyDrop(held))
            .testTag(controlTestTag(button))
            .size(size)
            .clip(shape)
            .background(background)
            .border(BloxBorders.frame, outline, shape)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                readingOrder?.let { traversalIndex = it }
                onClick {
                    controls.tap(button)
                    true
                }
            }
            .pointerInput(button, controls) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { change ->
                            if (change.changedToDownIgnoreConsumed()) controls.press(change.id.value, button)
                            if (change.changedToUpIgnoreConsumed()) controls.lift(change.id.value)
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(button, foreground, Modifier.graphicsLayer { rotationZ = glyphAngle() })
    }
}

/**
 * The glyph of a key: on B and A, their pictogram of rotation alone, the letter left to TalkBack; on the arrows, a
 * triangle, drawn, not typed: a Unicode arrow may be shown as an emoji.
 */
@Composable
private fun Glyph(
    button: ControlButton,
    color: Color,
    modifier: Modifier = Modifier,
) {
    when (button) {
        ControlButton.A -> BloxIcon(
            BloxIcon.ROTATE_RIGHT,
            size = ControlSizes.rotationGlyph,
            tint = color,
            modifier = modifier,
        )

        ControlButton.B -> BloxIcon(
            BloxIcon.ROTATE_LEFT,
            size = ControlSizes.rotationGlyph,
            tint = color,
            modifier = modifier,
        )

        else -> Spacer(
            modifier = modifier
                .size(ControlSizes.arrowGlyph)
                .drawWithCache {
                    val arrow = arrowPath(button, size.width / ARROW_GRID)
                    onDrawBehind { drawPath(arrow, color) }
                },
        )
    }
}

/** No key turns its icon: the keys without effects, as in Original. */
internal val NoKeyAngle: (ControlButton) -> Float = { 0f }
private val NoGlyphAngle: () -> Float = { 0f }

/** The grid the triangles are drawn on, as in the design system: 22 × 22. */
private const val ARROW_GRID = 22f

/** A triangle pointing the way of [button] (left, down or right), on the grid of [ARROW_GRID], of [unit] per step. */
private fun arrowPath(
    button: ControlButton,
    unit: Float,
): Path = Path().apply {
    when (button) {
        ControlButton.LEFT -> {
            moveTo(4 * unit, 11 * unit)
            lineTo(16 * unit, 4 * unit)
            lineTo(16 * unit, 18 * unit)
        }

        ControlButton.RIGHT -> {
            moveTo(18 * unit, 11 * unit)
            lineTo(6 * unit, 4 * unit)
            lineTo(6 * unit, 18 * unit)
        }

        else -> {
            moveTo(4 * unit, 5 * unit)
            lineTo(18 * unit, 5 * unit)
            lineTo(11 * unit, 18 * unit)
        }
    }
    close()
}

private fun descriptionOf(button: ControlButton): Int = when (button) {
    ControlButton.LEFT -> R.string.feature_game_control_left
    ControlButton.DOWN -> R.string.feature_game_control_down
    ControlButton.RIGHT -> R.string.feature_game_control_right
    ControlButton.A -> R.string.feature_game_control_a
    ControlButton.B -> R.string.feature_game_control_b
}

/** The controls as in the game screen: a phone of 412 dp, with its 16 dp margins; for the left hand when [leftHanded]. */
@Composable
private fun ControlsPreview(
    kind: BloxThemeKind,
    controls: HeldControls = remember { HeldControls {} },
    leftHanded: Boolean = false,
) {
    BloxTheme(kind = kind) {
        ControlPad(
            controls,
            Modifier.background(BloxTheme.palette.page).padding(ScreenMargin),
            leftHanded = leftHanded,
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 412)
@Composable
private fun GameControlsOriginalPreview() {
    ControlsPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 412)
@Composable
private fun GameControlsColorfulPreview() {
    ControlsPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 412)
@Composable
internal fun GameControlsHeldPreview() {
    val controls = remember {
        HeldControls {}.apply {
            press(finger = 1, ControlButton.LEFT)
            press(finger = 2, ControlButton.A)
        }
    }
    ControlsPreview(BloxThemeKind.COLORFUL, controls)
}

@Preview(widthDp = 412)
@Composable
internal fun GameControlsLeftHandedPreview() {
    ControlsPreview(BloxThemeKind.COLORFUL, leftHanded = true)
}
