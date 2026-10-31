package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.gloss

/**
 * A bubble button of [family] (docs/design-system/README.md, Boutons), in the colours, shape and relief of the theme:
 * a pill (a disc for an icon), square and flat in Original. Pressed, it takes its pressed fill and goes 2 dp down at
 * once, and comes back in `motion-press` once released; [enabled] false, it is flat, in `label` framed by
 * `panel-border`, and cannot be touched; focused by the keyboard, it bears a ring of 3 dp in `ink`. Its [content] is
 * drawn in its ink and the label style of its family, centred, [IconSizes.gap] apart; TalkBack reads it as one
 * button. A tile (Mes musiques, Composer) gives its [shape], `radius-md`, in place of the pill. Its [emphasis] says how
 * it stands out ([ButtonEmphasis]); [active], at work (Écouter while it plays), it keeps its pressed fill without
 * going down; its [size] may be compact, 48 dp high, for a main or a secondary button, and a narrow tile (the tools of
 * the editor) may keep less room on each side of its content, [sidePadding], than its family. TalkBack says what a touch
 * does, [onClickLabel], when its label alone does not (« La jouer pendant les parties »), and reads it with its [role]:
 * a button, or a radio button among others (the bars of the editor), selected when it is in [ButtonEmphasis.SELECTED].
 */
@Composable
fun BubbleButton(
    family: ButtonFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = BloxTheme.shapes.pill,
    emphasis: ButtonEmphasis = ButtonEmphasis.PLAIN,
    active: Boolean = false,
    size: ButtonSize = ButtonSize.REGULAR,
    sidePadding: Dp = family.sidePadding,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    PressableBubbleButton(
        family = family,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        emphasis = emphasis,
        active = active,
        size = size,
        onInk = false,
        modifier = modifier,
        sidePadding = sidePadding,
        role = role,
        onClickLabel = onClickLabel,
        content = content,
    )
}

/**
 * A bare button of [family] on the ink of a message (its action, its cross): nothing but its [content], in the colour
 * of the page, until it is pressed; [size] as for any bubble button.
 */
@Composable
internal fun BareButtonOnInk(
    family: ButtonFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.REGULAR,
    content: @Composable RowScope.() -> Unit,
) {
    PressableBubbleButton(
        family = family,
        onClick = onClick,
        modifier = modifier,
        enabled = true,
        shape = BloxTheme.shapes.pill,
        emphasis = ButtonEmphasis.BARE,
        active = false,
        size = size,
        onInk = true,
        content = content,
    )
}

/** A [BubbleButtonDrawing] that goes down and comes back up as its presses and releases say, [onInk] or not. */
@Composable
private fun PressableBubbleButton(
    family: ButtonFamily,
    onClick: () -> Unit,
    enabled: Boolean,
    shape: Shape,
    emphasis: ButtonEmphasis,
    active: Boolean,
    size: ButtonSize,
    onInk: Boolean,
    modifier: Modifier = Modifier,
    sidePadding: Dp = family.sidePadding,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val motion = rememberPressMotion(interactionSource)
    val focused by interactionSource.collectIsFocusedAsState()
    val depth = remember(motion) { { motion.depth } }
    BubbleButtonDrawing(
        family = family,
        onClick = onClick,
        interactionSource = interactionSource,
        depth = depth,
        modifier = modifier,
        enabled = enabled,
        focused = focused,
        emphasis = emphasis,
        active = active,
        size = size,
        shape = shape,
        onInk = onInk,
        sidePadding = sidePadding,
        role = role,
        onClickLabel = onClickLabel,
        content = content,
    )
}

/**
 * A bubble button of [family], [enabled], [focused] and [active] or not, standing out as [emphasis] says, as far down
 * as [depth] says, whatever its [interactionSource] says: the previews draw each state so, [BubbleButton] reads it from
 * its interactions. The depth is read only where the button is placed, so that its way back up moves it without
 * composing it again; its colours and relief change twice, when it goes down and when it is back. TalkBack reads it
 * with its [role], a selected one as selected, and what a touch does, [onClickLabel], if given. [onInk], it stands on
 * the ink of a message. Its content keeps [sidePadding] on each side.
 */
@Composable
internal fun BubbleButtonDrawing(
    family: ButtonFamily,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource?,
    depth: () -> Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focused: Boolean = false,
    emphasis: ButtonEmphasis = ButtonEmphasis.PLAIN,
    active: Boolean = false,
    size: ButtonSize = ButtonSize.REGULAR,
    shape: Shape = BloxTheme.shapes.pill,
    onInk: Boolean = false,
    sidePadding: Dp = family.sidePadding,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val down by remember(depth) { derivedStateOf { depth() > 0f } }
    val state = ButtonState(enabled, if (down) 1f else 0f, focused, active, emphasis, onInk)
    val effects = BloxTheme.effects
    val look = buttonLook(family, BloxTheme.buttons, BloxTheme.glosses, state)
    val frame = look.frame
    val frameWidth = if (frame == null) 0.dp else BloxBorders.frame
    Row(
        // The drop first: what the screen draws around the button, the ring of a chosen tile, goes down with it.
        modifier = Modifier
            .offset { IntOffset(0, buttonDrop(enabled, depth(), effects).roundToPx()) }
            .then(modifier)
            .focusRing(look.focusRing, shape)
            .sizeIn(minWidth = BloxSizes.minTouchTarget, minHeight = family.minHeight(size))
            .background(look.fill, shape)
            .gloss(look.gloss, shape, frameWidth)
            .then(if (frame == null) Modifier else Modifier.border(frameWidth, frame, shape))
            .selection(role, emphasis)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onClick = onClick,
            )
            .padding(horizontal = sidePadding),
        horizontalArrangement = Arrangement.spacedBy(IconSizes.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides look.ink,
            LocalTextStyle provides buttonLabelStyle(family),
        ) {
            content()
        }
    }
}

/**
 * What TalkBack reads of the selection of a button of [role] in [emphasis]: a choice among others (a radio button, a
 * tab) is selected or not; any other button is selected in the selected emphasis only, and says nothing otherwise.
 */
private fun Modifier.selection(
    role: Role,
    emphasis: ButtonEmphasis,
): Modifier {
    val chosen = emphasis == ButtonEmphasis.SELECTED
    val choice = role == Role.RadioButton || role == Role.Tab
    return if (chosen || choice) semantics { selected = chosen } else this
}

/** The main action of a screen (Reprendre, Rejouer): [icon], decorative, then [text]. */
@Composable
fun PrimaryButton(
    text: String,
    icon: BloxIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LabelledButton(ButtonFamily.PRIMARY, text, icon, onClick, modifier, enabled)
}

/** A secondary action of a screen (Menu, Changer): [icon], decorative, then [text]. */
@Composable
fun SecondaryButton(
    text: String,
    icon: BloxIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LabelledButton(ButtonFamily.SECONDARY, text, icon, onClick, modifier, enabled)
}

/** An action that cannot be undone (Effacer, the record), asked for before it is done: [icon], decorative, then [text]. */
@Composable
fun DangerButton(
    text: String,
    icon: BloxIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LabelledButton(ButtonFamily.DANGER, text, icon, onClick, modifier, enabled)
}

/** A button of [family] that shows [icon], decorative, then [text], centred on as many lines as it needs. */
@Composable
private fun LabelledButton(
    family: ButtonFamily,
    text: String,
    icon: BloxIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BubbleButton(family = family, onClick = onClick, modifier = modifier, enabled = enabled) {
        BloxIcon(icon)
        Text(text = text, textAlign = TextAlign.Center)
    }
}

/**
 * Between the sides of a button of this family and its content: none for a disc, which centres its pictogram. A screen
 * that decides whether labels hold in its buttons counts it.
 */
val ButtonFamily.sidePadding: Dp
    get() = if (this == ButtonFamily.ICON) 0.dp else BloxSpaces.space6

/**
 * The style of the label of a button of [family]: `play-label` for Jouer, `button-label-strong` for a main button,
 * `button-label` otherwise, over the Material label, whose size Original keeps. A screen that decides whether labels
 * hold in its buttons measures them in it.
 */
@Composable
@ReadOnlyComposable
fun buttonLabelStyle(family: ButtonFamily): TextStyle {
    val typography = BloxTheme.typography
    val style = when (family) {
        ButtonFamily.PRIMARY_XL -> typography.playLabel

        ButtonFamily.PRIMARY -> typography.buttonLabelStrong

        ButtonFamily.SECONDARY, ButtonFamily.DANGER, ButtonFamily.DANGER_FILLED, ButtonFamily.ICON ->
            typography.buttonLabel
    }
    return MaterialTheme.typography.labelLarge.merge(style)
}

/**
 * How far down a button is, as its presses and releases say: all the way at once when pressed, back up in
 * [returnTicks] ticks once released, counted on the frames of the screen (ADR 0006 and 0012). A press and its release
 * in one frame, as a brief tap in a parent that scrolls (whose press is held back until the finger lifts), still send
 * it down, then back up; in reduced effects there is no way back ([pressReturnTicks]), and such a tap may not show.
 */
@Stable
internal class PressMotion(private val returnTicks: Int) {
    private var pressed by mutableStateOf(false)
    private var ticksSinceRelease by mutableIntStateOf(returnTicks)

    /** How many times the button was let go: each one starts its way back up. */
    var releases by mutableIntStateOf(0)
        private set

    val depth: Float
        get() = pressDepth(pressed, ticksSinceRelease, returnTicks)

    fun press() {
        pressed = true
        ticksSinceRelease = 0
    }

    fun release() {
        pressed = false
        ticksSinceRelease = 0
        releases++
    }

    /** Counts the ticks of the way back up, frame by frame, until it is over or the button is pressed again. */
    suspend fun comeBack() {
        val frames = FrameTicks()
        while (!pressed && ticksSinceRelease < returnTicks) {
            withFrameNanos { ticksSinceRelease += frames.advance(it) }
        }
    }
}

/**
 * The [PressMotion] of the button of [interactionSource], fed by each of its presses, releases and cancels; a new one,
 * back at once, when the effects become reduced.
 */
@Composable
internal fun rememberPressMotion(interactionSource: InteractionSource): PressMotion {
    val returnTicks = pressReturnTicks(BloxTheme.effects)
    val motion = remember(returnTicks) { PressMotion(returnTicks) }
    LaunchedEffect(interactionSource, motion) {
        val presses = mutableListOf<PressInteraction.Press>()
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    presses += interaction
                    motion.press()
                }

                is PressInteraction.Release -> if (presses.remove(interaction.press) &&
                    presses.isEmpty()
                ) {
                    motion.release()
                }

                is PressInteraction.Cancel -> if (presses.remove(interaction.press) &&
                    presses.isEmpty()
                ) {
                    motion.release()
                }
            }
        }
    }
    LaunchedEffect(motion, motion.releases) { motion.comeBack() }
    return motion
}
