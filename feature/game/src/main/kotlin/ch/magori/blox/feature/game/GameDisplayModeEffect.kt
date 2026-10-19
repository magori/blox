package ch.magori.blox.feature.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Display
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

/**
 * While it is composed, asks the window of the activity for the mode of its display that [gameDisplayMode]
 * picks, 60 Hz on a screen at 90 or 120 Hz; once it leaves the composition, asks for the mode the window
 * asked for before. Nothing is asked when no mode suits the game, or when there is no activity window.
 *
 * The game screen holds it as long as it shows, paused or over included: changing the mode of the display
 * at each pause could make the screen blink. Two game screens may show together, the old and the new one
 * while Rejouer goes from one to the other: the window asks for the mode until the last of them is gone,
 * and only then for the mode it asked for before the first one came.
 *
 * `preferredDisplayModeId` (API 23) works from the minimal API of the application on, and is a request
 * of the application that the system grants before its own choices, unlike a frame rate vote
 * (`Modifier.preferredFrameRate`, which needs API 35). The system grants it only to the window in front:
 * in multi-window, the display follows the window the player is using.
 */
@Composable
internal fun GameDisplayModeEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val mode = view.display?.gameDisplayMode()
        if (window == null || mode == null) {
            onDispose {}
        } else {
            window.holdDisplayMode(mode.id)
            onDispose { window.releaseDisplayMode() }
        }
    }
}

/** The mode of this display the game asks for ([gameDisplayMode]); null when none suits it. */
internal fun Display.gameDisplayMode(): DisplayMode? = gameDisplayMode(
    current = mode.toDisplayMode(),
    supported = supportedModes.map(Display.Mode::toDisplayMode),
)

private fun Display.Mode.toDisplayMode(): DisplayMode = DisplayMode(modeId, physicalWidth, physicalHeight, refreshRate)

/** The mode a window asked for before the game, and how many game screens of the window hold the game's. */
private class DisplayModeHold(val previous: Int) {
    var holders = 0
}

/** The holds of the windows, read and written on the main thread only; a window gone takes its hold with it. */
private val displayModeHolds = WeakHashMap<Window, DisplayModeHold>()

/** Asks this window for the mode [modeId]; the first hold keeps the mode the window asked for before. */
private fun Window.holdDisplayMode(modeId: Int) {
    displayModeHolds.getOrPut(this) { DisplayModeHold(attributes.preferredDisplayModeId) }.holders++
    preferDisplayMode(modeId)
}

/** Ends a hold of [holdDisplayMode]: the last one asks this window for the mode it asked for before. */
private fun Window.releaseDisplayMode() {
    val hold = displayModeHolds[this] ?: return
    hold.holders--
    if (hold.holders == 0) {
        displayModeHolds.remove(this)
        preferDisplayMode(hold.previous)
    }
}

private fun Window.preferDisplayMode(modeId: Int) {
    attributes = attributes.apply { preferredDisplayModeId = modeId }
}

/** The activity of this context, unwrapping the wrappers around it; null when there is none. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
