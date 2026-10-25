package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.gloss
import ch.magori.blox.core.designsystem.theme.grownOutline

/**
 * A bubble button of [family] (docs/design-system/README.md, Boutons), in the colours, shape and relief of the theme:
 * a pill (a disc for an icon), square and flat in Original. Pressed, it takes its pressed fill and goes 2 dp down at
 * once, and comes back in `motion-press` once released; [enabled] false, it is flat, in `label` framed by
 * `panel-border`, and cannot be touched; focused by the keyboard, it bears a ring of 3 dp in `ink`. Its [content] is
 * drawn in its ink and the label style of its family, centred, [IconSizes.gap] apart; TalkBack reads it as one
 * button.
 */
@Composable
fun BubbleButton(
    family: ButtonFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val depth = rememberPressDepth(interactionSource)
    val focused by interactionSource.collectIsFocusedAsState()
    BubbleButton(family, ButtonState(enabled, depth, focused), onClick, interactionSource, modifier, content)
}

/**
 * A bubble button of [family] drawn in [state], whatever its [interactionSource] says: the previews draw each state
 * so, the button above reads it from its interactions.
 */
@Composable
internal fun BubbleButton(
    family: ButtonFamily,
    state: ButtonState,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val look = buttonLook(family, BloxTheme.buttons, BloxTheme.glosses, state)
    val shape = BloxTheme.shapes.pill
    val frame = look.frame
    val frameWidth = if (frame == null) 0.dp else BloxBorders.frame
    Row(
        modifier = modifier
            .offset { IntOffset(0, look.drop.roundToPx()) }
            .focusRing(look.focusRing, shape)
            .sizeIn(minWidth = family.minWidth, minHeight = family.minHeight)
            .background(look.fill, shape)
            .gloss(look.gloss, shape, frameWidth)
            .then(if (frame == null) Modifier else Modifier.border(frameWidth, frame, shape))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = state.enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = family.sidePadding),
        horizontalArrangement = Arrangement.spacedBy(IconSizes.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides look.ink, LocalTextStyle provides labelStyle(family)) {
            content()
        }
    }
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

/** Between the sides of a button of this family and its content: none for a disc, which centres its pictogram. */
private val ButtonFamily.sidePadding: Dp
    get() = if (this == ButtonFamily.ICON) 0.dp else BloxSpaces.space6

/**
 * The style of the label of a button of [family]: `play-label` for Jouer, `button-label-strong` for a main button,
 * `button-label` otherwise, over the Material label, whose size Original keeps.
 */
@Composable
@ReadOnlyComposable
private fun labelStyle(family: ButtonFamily): TextStyle {
    val typography = BloxTheme.typography
    val style = when (family) {
        ButtonFamily.PRIMARY_XL -> typography.playLabel
        ButtonFamily.PRIMARY -> typography.buttonLabelStrong
        ButtonFamily.SECONDARY, ButtonFamily.ICON -> typography.buttonLabel
    }
    return MaterialTheme.typography.labelLarge.merge(style)
}

/**
 * How far down the button of [interactionSource] is: all the way at once when pressed, back up in [returnTicks]
 * ticks once released, counted on the frames of the screen (ADR 0006 and 0012). [returnTicks] is where reduced
 * effects (P5.71) may ask for no return at all.
 */
@Composable
private fun rememberPressDepth(
    interactionSource: InteractionSource,
    returnTicks: Int = BloxMotion.press,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    var ticksSinceRelease by remember { mutableIntStateOf(returnTicks) }
    LaunchedEffect(pressed) {
        if (pressed) {
            ticksSinceRelease = 0
        } else {
            val frames = FrameTicks()
            while (ticksSinceRelease < returnTicks) {
                withFrameNanos { ticksSinceRelease += frames.advance(it) }
            }
        }
    }
    return pressDepth(pressed, ticksSinceRelease, returnTicks)
}

/** Draws [ring], if any, around [shape], off its edge: drawn outside the bounds, it takes no room. */
private fun Modifier.focusRing(
    ring: FocusRing?,
    shape: Shape,
): Modifier = if (ring == null) {
    this
} else {
    drawBehind {
        val width = ring.width.toPx()
        val outline = grownOutline(shape, size, ring.gap.toPx() + width / 2, layoutDirection, this)
        drawPath(Path().apply { addOutline(outline) }, ring.color, style = Stroke(width))
    }
}
