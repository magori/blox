package ch.magori.blox.feature.game.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.R

private val ArrowSize = 20.dp

/**
 * The letters A and B: bold and of 20 sp, large text for WCAG 2.1 (14 pt bold, 18.66 sp), so that their ink
 * needs a contrast of 3:1 on the key, which `button` and `accent` give in both themes, held or not.
 */
internal val KeyLetterStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)

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

/** ◀ ▼ ▶, half as low as B: [ControlSizes.directionsWidth] × [ControlSizes.directionsHeight]. */
@Composable
internal fun DirectionPad(
    controls: HeldControls,
    modifier: Modifier = Modifier,
) {
    val keyModifier = Modifier.padding(top = ControlSizes.rotationStagger / 2)
    ControlGroup(modifier = modifier) {
        ControlKey(ControlButton.LEFT, controls, ControlSizes.directionButton, keyModifier)
        ControlKey(ControlButton.DOWN, controls, ControlSizes.directionButton, keyModifier)
        ControlKey(ControlButton.RIGHT, controls, ControlSizes.directionButton, keyModifier)
    }
}

/** B and A, B lower: [ControlSizes.rotationsWidth] × [ControlSizes.rotationsHeight]. */
@Composable
internal fun RotationPad(
    controls: HeldControls,
    modifier: Modifier = Modifier,
) {
    ControlGroup(modifier = modifier) {
        ControlKey(
            ControlButton.B,
            controls,
            ControlSizes.rotationButton,
            Modifier.padding(top = ControlSizes.rotationStagger),
        )
        ControlKey(ControlButton.A, controls, ControlSizes.rotationButton)
    }
}

/**
 * The keys of a group in a row, [ControlSizes.spacing] apart. The group is excluded from the system
 * gestures (API 29 and above): in gesture navigation, the zone of the Back gesture can be wider than
 * the margins of the screen, and a thumb sliding from ◀ to ▼ would otherwise go back, which cancels
 * the fingers and pauses the game. The system honours 200 dp of exclusion per edge, which the groups
 * fit: ◀ ▼ ▶ is [ControlSizes.directionsHeight] high, B A [ControlSizes.rotationsHeight], and both
 * together would still fit on one edge.
 */
@Composable
private fun ControlGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.systemGestureExclusion(),
        horizontalArrangement = Arrangement.spacedBy(ControlSizes.spacing),
        content = { content() },
    )
}

/** Both groups on one row, as on a 360 dp wide phone, for the previews. */
@Composable
private fun ControlPad(
    controls: HeldControls,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        DirectionPad(controls)
        RotationPad(controls)
    }
}

/**
 * The tag of a key (`control_left`, `control_a`…), the same in every language: the measured journey of
 * `:benchmark` finds the keys by it (`By.res`, with `testTagsAsResourceId` set by `app`).
 */
internal fun controlTestTag(button: ControlButton): String = "control_${button.name.lowercase()}"

@Composable
private fun ControlKey(
    button: ControlButton,
    controls: HeldControls,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val (background, outline, foreground) =
        keyColors(BloxTheme.palette, button, held = button in controls.heldButtons)
    val description = stringResource(descriptionOf(button))
    val shape = BloxTheme.shapes.button
    Box(
        modifier = modifier
            .testTag(controlTestTag(button))
            .size(size)
            .clip(shape)
            .background(background)
            .border(BloxBorders.frame, outline, shape)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
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
        Glyph(button, foreground)
    }
}

/** The arrows are drawn, not typed: a Unicode arrow may be shown as an emoji. */
@Composable
private fun Glyph(
    button: ControlButton,
    color: Color,
    modifier: Modifier = Modifier,
) {
    when (button) {
        ControlButton.A -> Text(text = "A", modifier = modifier, color = color, style = KeyLetterStyle)

        ControlButton.B -> Text(text = "B", modifier = modifier, color = color, style = KeyLetterStyle)

        else -> Spacer(
            modifier = modifier
                .size(ArrowSize)
                .drawWithCache {
                    val arrow = arrowPath(button, size.width, size.height)
                    onDrawBehind { drawPath(arrow, color) }
                },
        )
    }
}

/** A triangle pointing the way of [button] (left, down or right), filling [width] × [height]. */
private fun arrowPath(
    button: ControlButton,
    width: Float,
    height: Float,
): Path = Path().apply {
    when (button) {
        ControlButton.LEFT -> {
            moveTo(0f, height / 2)
            lineTo(width, 0f)
            lineTo(width, height)
        }

        ControlButton.RIGHT -> {
            moveTo(width, height / 2)
            lineTo(0f, 0f)
            lineTo(0f, height)
        }

        else -> {
            moveTo(width / 2, height)
            lineTo(0f, 0f)
            lineTo(width, 0f)
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

/** The controls as in the game screen: a 360 dp wide phone, with its 24 dp margins. */
@Composable
private fun ControlsPreview(
    kind: BloxThemeKind,
    controls: HeldControls = remember { HeldControls {} },
) {
    BloxTheme(kind = kind) {
        ControlPad(controls, Modifier.background(BloxTheme.palette.page).padding(24.dp))
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360)
@Composable
private fun GameControlsOriginalPreview() {
    ControlsPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360)
@Composable
private fun GameControlsColorfulPreview() {
    ControlsPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360)
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
