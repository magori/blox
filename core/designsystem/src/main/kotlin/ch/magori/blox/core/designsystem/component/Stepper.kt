package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.BloxPalette
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The colours of a slider or a gauge: [reached], up to its value, [beyond], past it, and the [frame] of its track and of
 * its knob, if they have one of their own.
 */
@Immutable
internal data class StepColors(val reached: Color, val beyond: Color, val frame: Color? = null)

/**
 * The [StepColors] of [palette]: `accent` up to the value, its [quietColor] beyond it.
 */
internal fun stepColors(palette: BloxPalette): StepColors =
    StepColors(reached = palette.accent, beyond = quietColor(palette))

/**
 * The [StepColors] of a disabled slider in [palette], flat and quiet: its [quietColor] up to the value, `panel-inset`
 * beyond it, the track and the knob framed in its [quietColor], as a disabled switch, so that the track still shows on
 * the page (`panel-inset` alone is 1.02:1 on it by day).
 */
internal fun disabledStepColors(palette: BloxPalette): StepColors = quietColor(palette).let { quiet ->
    StepColors(reached = quiet, beyond = palette.panelInset, frame = quiet)
}

/**
 * `panel-border`, but where the palette has it the same as its `accent` (Original, whose frames are in its ink),
 * `button`: what is not reached, or not enabled, must not look like what is.
 */
private fun quietColor(palette: BloxPalette): Color =
    if (palette.panelBorder == palette.accent) palette.button else palette.panelBorder

/**
 * What TalkBack reads of a slider at [value]: one slider, [description], then [state], whose steps are those of the
 * value, so that TalkBack moves it one step at a time (by its range over its steps plus one); a value it asks for
 * gives the nearest step, passed to [onValueChange] if it is another. Not [enabled], TalkBack reads it unavailable and
 * cannot move it.
 */
internal fun Modifier.stepSemantics(
    value: SteppedValue,
    description: String,
    state: String,
    enabled: Boolean = true,
    onValueChange: (SteppedValue) -> Unit,
): Modifier = semantics(mergeDescendants = true) {
    contentDescription = description
    stateDescription = state
    progressBarRangeInfo = ProgressBarRangeInfo(
        current = value.value.toFloat(),
        range = value.steps.first.toFloat()..value.steps.last.toFloat(),
        steps = (value.lastIndex - 1).coerceAtLeast(0),
    )
    if (enabled) {
        setProgress { target ->
            val nearest = value.nearestToValue(target)
            if (nearest != value) onValueChange(nearest)
            true
        }
    } else {
        disabled()
    }
}

/**
 * Moves a slider at [value] where it is touched or dragged to: the nearest step to the finger, along the way from
 * [inset] off the start to [inset] off the end, in [layoutDirection] (where the centre of the knob, or of the first and
 * last bars, goes), passed to [onValueChange] if it is another; a finger before the start or past the end gives the
 * first or the last step. A drag starts only once the finger has moved sideways, so that a parent that scrolls still
 * scrolls. [value] and [onValueChange] are read at each touch: a drag goes on while the value it changes is composed
 * again. During a drag, [gesture] holds the step under the finger: the slider shows it, and a step is asked for once,
 * when the finger reaches it, even while the value given back lags (a write that takes its time); once the finger
 * lifts, the slider shows its value again. Both detectors start at once, not dispatched: the first touch, which starts
 * them, is not lost.
 */
internal fun Modifier.stepTouch(
    inset: Dp,
    layoutDirection: LayoutDirection,
    value: () -> SteppedValue,
    gesture: StepGesture,
    onValueChange: (SteppedValue) -> Unit,
): Modifier = pointerInput(inset, layoutDirection, gesture) {
    val moveTo = { x: Float ->
        val start = inset.toPx()
        val along = (x - start) / (size.width - 2 * start).coerceAtLeast(1f)
        val current = gesture.shown(value())
        val nearest = current.nearestTo(if (layoutDirection == LayoutDirection.Rtl) 1 - along else along)
        if (nearest != current) {
            if (gesture.held != null) gesture.held = nearest
            onValueChange(nearest)
        }
    }
    coroutineScope {
        launch(start = CoroutineStart.UNDISPATCHED) { detectTapGestures { moveTo(it.x) } }
        launch(start = CoroutineStart.UNDISPATCHED) {
            detectHorizontalDragGestures(
                onDragStart = {
                    gesture.held = value()
                    moveTo(it.x)
                },
                onDragEnd = { gesture.held = null },
                onDragCancel = { gesture.held = null },
            ) { change, _ ->
                change.consume()
                moveTo(change.position.x)
            }
        }
    }
}

/**
 * What a finger and the keyboard do with a slider. The step a finger holds while it drags the slider, [held], or null
 * when no finger drags it: what the slider shows then, rather than its value, which lags behind the finger until the
 * value asked for is written and given back. Whether the slider has the focus of the keyboard, [focused]. The step
 * arrows last asked for, so that the next arrow goes on from it while the value has not come back ([fromKeys]), and the
 * arrow whose press the slider took, [takenKey], so that it takes its release too.
 */
@Stable
internal class StepGesture {
    var held: SteppedValue? by mutableStateOf(null)
    var focused: Boolean by mutableStateOf(false)

    /** The arrow whose press the slider took, until its release: read by the keys only, not drawn. */
    var takenKey: Key? = null

    /**
     * Where the arrows started from, and the step they last asked for: read by the keys only, not drawn. The value
     * comes back on its way from the one to the other, late, a step at a time or at once.
     */
    private var keyed: Pair<SteppedValue, SteppedValue>? = null

    /** What a slider at [value] shows: the step held by the finger, if any, else [value]. */
    fun shown(value: SteppedValue): SteppedValue = held ?: value

    /**
     * Where the next arrow moves a slider at [value] from: the step the arrows last asked for, as long as the slider
     * shows a step on the way to it, from where they started (the value has not all come back); else what it shows,
     * the step asked for being forgotten, once reached or once the value has left the way to it.
     */
    fun fromKeys(value: SteppedValue): SteppedValue {
        val shown = shown(value)
        val (start, asked) = keyed ?: return shown
        val way = minOf(start.value, asked.value)..maxOf(start.value, asked.value)
        if (shown == asked || shown.value !in way) {
            keyed = null
            return shown
        }
        return asked
    }

    /**
     * Notes [asked], asked for by an arrow while the slider at [value] showed what it shows: on the way from where the
     * arrows started, if the step they asked for before is not forgotten ([fromKeys]), else from what it shows.
     */
    fun keyAsked(
        value: SteppedValue,
        asked: SteppedValue,
    ) {
        keyed = (keyed?.first ?: shown(value)) to asked
    }
}

/** The [StepGesture] of a slider, kept across its compositions. */
@Composable
internal fun rememberStepGesture(): StepGesture = remember { StepGesture() }

/**
 * What a slider does with the step it is asked for: gives its value to [onChange], the one of the last composition,
 * through a function that stays the same, so that the touch and TalkBack, set up once, always call the current one.
 */
@Composable
internal fun rememberStepChange(onChange: (Int) -> Unit): (SteppedValue) -> Unit {
    val latest by rememberUpdatedState(onChange)
    return remember { { next: SteppedValue -> latest(next.value) } }
}

/**
 * Lets the keyboard or a pad focus a slider at [value], and move it a step by left or right ([afterArrow]) in
 * [layoutDirection], as a seek bar of Android: the step asked for is passed to [onValueChange], and the next arrow goes
 * on from it while the value has not come back ([StepGesture.fromKeys]). An arrow that would not move it (at the end
 * of its range), up and down, and the arrows of the buttons it holds (− and + of a gauge) are left to the focus, so
 * that a pad, which has no Tab, can always leave it; but an arrow held down stops at the end of the range, its repeats
 * taken, and only a new press leaves it. The release of an arrow whose press it took is taken too. Focused, the slider
 * bears the ring of the buttons
 * ([buttonFocusRing] of [buttons]) around [shape]; not [enabled], it takes no focus. [value] and [onValueChange] are
 * read at each key, as for a touch.
 */
internal fun Modifier.stepKeys(
    layoutDirection: LayoutDirection,
    buttons: BloxButtonColors,
    shape: Shape,
    gesture: StepGesture,
    onValueChange: (SteppedValue) -> Unit,
    enabled: Boolean = true,
    value: () -> SteppedValue,
): Modifier = this
    .focusRing(shape) { buttonFocusRing(gesture.focused, buttons) }
    .onFocusChanged { gesture.focused = it.isFocused }
    .onKeyEvent { event ->
        val arrow = event.arrow()
        if (!gesture.focused || arrow == null) return@onKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> gesture.arrowDown(event, arrow, layoutDirection, onValueChange, value())
            KeyEventType.KeyUp -> (gesture.takenKey == event.key).also { gesture.takenKey = null }
            else -> false
        }
    }
    .focusable(enabled)

/**
 * Moves a slider at [given] a step by [arrow], pressed or repeated by [event], and says whether the slider takes it:
 * when it moves, or when it is a repeat at the end of the range, so that a held arrow does not leave it.
 */
private fun StepGesture.arrowDown(
    event: KeyEvent,
    arrow: Arrow,
    layoutDirection: LayoutDirection,
    onValueChange: (SteppedValue) -> Unit,
    given: SteppedValue,
): Boolean {
    val current = fromKeys(given)
    val next = current.afterArrow(arrow, layoutDirection)
    val taken = next != current || event.nativeKeyEvent.repeatCount > 0
    if (next != current) {
        keyAsked(given, next)
        onValueChange(next)
    }
    takenKey = if (taken) event.key else null
    return taken
}

/** The arrow of this key, or null if it is not an arrow. */
private fun KeyEvent.arrow(): Arrow? = when (key) {
    Key.DirectionLeft -> Arrow.LEFT
    Key.DirectionRight -> Arrow.RIGHT
    else -> null
}

/**
 * An arrow along a slider, of a keyboard or of the cross of a pad. Up and down are not: they move the focus, as for a
 * seek bar of Android, so that a pad, which has no Tab, can leave a slider in a column.
 */
internal enum class Arrow { LEFT, RIGHT }

/**
 * The step [arrow] asks of a slider at this value, never out of its range: it moves it the way it points along the
 * track, which starts on the left, or on the right in a [layoutDirection] read from right to left.
 */
internal fun SteppedValue.afterArrow(
    arrow: Arrow,
    layoutDirection: LayoutDirection,
): SteppedValue {
    val towardsEnd = if (layoutDirection == LayoutDirection.Rtl) Arrow.LEFT else Arrow.RIGHT
    return if (arrow == towardsEnd) increased() else decreased()
}
