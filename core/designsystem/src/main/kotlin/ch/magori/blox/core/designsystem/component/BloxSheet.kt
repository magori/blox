package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/*
 * The sheets of the screens (ADR 0026): a panel risen from the bottom of the screen, over a scrim, in a window of its
 * own, over the whole screen, so that TalkBack reads it alone, as a dialog. Its opening is the caller's: the state of
 * the screen, or its ViewModel, composes it while it is open.
 */

/**
 * A sheet over the screen, risen from its bottom: a handle, its [title], read first by TalkBack, as a heading, then its
 * [content], which scrolls when it is too high, and its [actions], under it, always in sight. It rises in `motion-card`
 * on the frames of the screen, or fades in within `motion-reduced-fade` in reduced effects. The system back and a
 * touch on its scrim call [onDismissRequest]; it does not slide away under the finger. It stands above the keyboard
 * and the bars of the system, which its panel goes behind.
 */
@Composable
fun BloxSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BloxSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        insets = WindowInsets.safeDrawing,
        modifier = modifier,
        actions = actions,
        content = content,
    )
}

/** A sheet as above, kept away from [insets] (the keyboard and the bars of the system, or those of a test). */
@Composable
internal fun BloxSheet(
    title: String,
    onDismissRequest: () -> Unit,
    insets: WindowInsets,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            windowTitle = title,
        ),
    ) {
        OwnMotionAndScrim()
        val effects = BloxTheme.effects
        val ticks = rememberEntranceTicks(effects)
        SheetFrame(
            title = title,
            onDismissRequest = onDismissRequest,
            insets = insets,
            entrance = { sheetEntrance(ticks(), effects) },
            modifier = modifier,
            actions = actions,
            content = content,
        )
    }
}

/**
 * Leaves the sheet alone to come in and to darken the screen: its window neither dims what is behind it nor plays the
 * animation of the platform, which would come on top of `motion-card` and stay in reduced effects.
 */
@Composable
private fun OwnMotionAndScrim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        window?.setDimAmount(0f)
        window?.setWindowAnimations(0)
    }
}

/**
 * The ticks since the sheet opened, counted on the frames of the screen (ADR 0012) until it has come in: read where it
 * is drawn, they compose nothing again.
 */
@Composable
private fun rememberEntranceTicks(effects: BloxEffects): () -> Int {
    var ticks by remember { mutableIntStateOf(0) }
    LaunchedEffect(effects) {
        val frames = FrameTicks()
        while (sheetEntrance(ticks, effects) != SheetEntrance.Settled) {
            withFrameNanos { ticks += frames.advance(it) }
        }
    }
    return remember { { ticks } }
}

/**
 * What a sheet draws over the screen, without its window: the previews draw it so. Its scrim and its panel take what
 * [entrance] says when they are drawn.
 */
@Composable
internal fun SheetFrame(
    title: String,
    onDismissRequest: () -> Unit,
    insets: WindowInsets,
    entrance: () -> SheetEntrance,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val dismiss by rememberUpdatedState(onDismissRequest)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // A touch on the scrim closes the sheet. TalkBack does not read it: it goes back instead.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(palette.scrim, alpha = entrance().scrimAlpha) }
                .pointerInput(Unit) { detectTapGestures { dismiss() } },
        )
        val shape = sheetShape(BloxTheme.shapes.card)
        Column(
            modifier = modifier
                .windowInsetsPadding(insets.only(WindowInsetsSides.Top))
                .padding(top = BloxSizes.minTouchTarget)
                .fillMaxWidth()
                .graphicsLayer {
                    val now = entrance()
                    translationY = now.rise * SHEET_RISE.toPx()
                    alpha = now.panelAlpha
                }
                // A touch on the panel stays on it.
                .pointerInput(Unit) { detectTapGestures() }
                .background(palette.panel, shape)
                .frameOpenAtBottom(BloxBorders.frame, palette.panelBorder, shape)
                .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(start = BloxSpaces.space4, end = BloxSpaces.space4, top = BloxSpaces.space2)
                .padding(bottom = BloxSpaces.space4),
        ) {
            SheetHandle(palette.label)
            Text(
                text = title,
                color = palette.ink,
                // Cut at a syllable, never in the middle of one, when a word is wider than the sheet.
                style = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.screenTitle)
                    .copy(hyphens = Hyphens.Auto),
                modifier = Modifier
                    .padding(bottom = BloxSpaces.space1)
                    .semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
            if (actions != null) {
                Column(
                    modifier = Modifier.padding(top = BloxSpaces.space2),
                    verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
                    content = actions,
                )
            }
        }
    }
}

/** The handle of a sheet, in [color], faded: a cue that it came from below, which nothing reads. */
@Composable
private fun ColumnScope.SheetHandle(color: Color) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 2.dp, bottom = 6.dp)
            .size(HANDLE_WIDTH, HANDLE_HEIGHT)
            .background(color.copy(alpha = HANDLE_ALPHA), BloxTheme.shapes.pill),
    )
}

/**
 * The frame of a panel standing on the bottom of the screen, [width] wide in [color] around [shape], open at its
 * bottom: its sides go down to the edge, where nothing closes it.
 */
private fun Modifier.frameOpenAtBottom(
    width: Dp,
    color: Color,
    shape: Shape,
): Modifier = drawWithCache {
    val stroke = width.toPx()
    // The outline of a panel taller than the box by a stroke, whose bottom side is drawn out of it, then clipped.
    val outline = shape.createOutline(Size(size.width - stroke, size.height + stroke), layoutDirection, this)
    onDrawBehind {
        clipRect {
            translate(stroke / 2, stroke / 2) { drawOutline(outline, color, style = Stroke(stroke)) }
        }
    }
}

/** How far below its place a sheet starts to rise (as the maquette's sheets do). */
private val SHEET_RISE = 40.dp

/** The handle: 32 by 4 dp, in `label` at 60 %. */
private val HANDLE_WIDTH = 32.dp
private val HANDLE_HEIGHT = 4.dp
private const val HANDLE_ALPHA = 0.6f
