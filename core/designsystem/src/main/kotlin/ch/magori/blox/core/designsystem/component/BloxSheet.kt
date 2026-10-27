package ch.magori.blox.core.designsystem.component

import android.view.ContextThemeWrapper
import android.view.View
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/*
 * The sheets of the screens (ADR 0029): a panel risen from the bottom of the screen, over a scrim, in a window of its
 * own, over the whole screen, so that TalkBack reads it alone, as a dialog. Its opening is the caller's: the state of
 * the screen, or its ViewModel, composes it while it is open.
 */

/**
 * A sheet over the screen, risen from its bottom: a handle, its [title], read first by TalkBack, as a heading, and its
 * [content], which scroll together when they are too high, then its [actions], under them, always in sight. It rises
 * in `motion-card` on the frames of the screen, or fades in within `motion-reduced-fade` in reduced effects; once come
 * in, it stays in place when its screen is recreated (a rotation). The system back and a touch on its scrim call
 * [onDismissRequest], once for the sheet; it does not slide away under the finger. It stands above the keyboard and
 * the bars of the system, which its panel goes behind. [modifier] applies to its panel.
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
    val dismissOnce = rememberOnce(onDismissRequest)
    val effects = BloxTheme.effects
    val ticks = rememberEntranceTicks(effects)
    SheetWindowTheme {
        Dialog(
            onDismissRequest = dismissOnce,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                windowTitle = title,
            ),
        ) {
            SheetFrame(
                title = title,
                onDismissRequest = dismissOnce,
                insets = insets,
                entrance = { sheetEntrance(ticks(), effects) },
                modifier = modifier,
                actions = actions,
                content = content,
            )
        }
    }
}

/**
 * Gives the window of a sheet its theme before it shows (`CoreDesignsystem.SheetWindow`): it neither dims what is
 * behind it, the sheet drawing its own scrim, nor plays the animation of the platform, which would come on top of
 * `motion-card` and stay in reduced effects. A dialog takes the theme of its window from the view it opens from: here
 * a stand-in of the view of the screen, in its context given that theme, with its owners.
 */
@Composable
private fun SheetWindowTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val themed = remember(view) { sheetWindowAnchor(view) }
    CompositionLocalProvider(LocalView provides themed, content = content)
}

/** A stand-in of [view], the view of a screen, which a sheet opens its window from: see [SheetWindowTheme]. */
private fun sheetWindowAnchor(view: View): View =
    View(ContextThemeWrapper(view.context, R.style.CoreDesignsystem_SheetWindow)).apply {
        setViewTreeLifecycleOwner(view.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(view.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(view.findViewTreeSavedStateRegistryOwner())
    }

/**
 * [action], done the first time it is asked for and never again while the caller stays composed: a sheet asks once to
 * close, its action is done once though touched twice before it goes.
 */
@Composable
fun rememberOnce(action: () -> Unit): () -> Unit {
    val current by rememberUpdatedState(action)
    return remember {
        var done = false
        {
            if (!done) {
                done = true
                current()
            }
        }
    }
}

/**
 * The ticks since the sheet opened, counted on the frames of the screen (ADR 0012) until it has come in: read where it
 * is drawn, they compose nothing again. Saved with the screen, a sheet come in does not come in again once it is
 * recreated.
 */
@Composable
private fun rememberEntranceTicks(effects: BloxEffects): () -> Int {
    var ticks by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(effects) {
        val frames = FrameTicks()
        while (sheetEntrance(ticks, effects) != SheetEntrance.Settled) {
            withFrameNanos { ticks += frames.advance(it) }
        }
    }
    return remember { { ticks } }
}

/**
 * A [BloxSheet] as it stands once come in, over what is drawn under it, without its window nor the bars of the system:
 * what the previews of the screens draw, a window not showing in their screenshots. Touching it does nothing.
 */
@Composable
fun SettledSheet(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetFrame(
        title = title,
        onDismissRequest = {},
        insets = WindowInsets(0),
        entrance = { SheetEntrance.Settled },
        modifier = modifier,
        actions = actions,
        content = content,
    )
}

/**
 * What a sheet draws over the screen, without its window: the previews draw it so. Its scrim and its panel take what
 * [entrance] says when they are drawn; [modifier] applies to its panel.
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
            // The title scrolls with the content: in a large font, over the keyboard, the actions keep their room.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                PanelTitle(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.screenTitle),
                    modifier = Modifier.padding(bottom = BloxSpaces.space1),
                )
                content()
            }
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
