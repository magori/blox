package ch.magori.blox.core.designsystem.test

import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.test.platform.app.InstrumentationRegistry

/**
 * The system back, handed to the dispatcher of [this] owner (an activity, or the `ComponentDialog` of a sheet) as the
 * platform does once it has read the gesture or the button, rather than injected as a key: an injected key goes
 * through the system server to whichever window holds the focus, and is lost when another window takes it or the
 * server stalls. Done on the main thread, returns once the dispatcher has handed it on; so not to be called from the
 * main thread, where `runOnMainSync` throws.
 */
fun OnBackPressedDispatcherOwner.pressSystemBack() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync { onBackPressedDispatcher.onBackPressed() }
}

/**
 * The system back, handed to the dispatcher of the window that shows [this] node, as above: the activity for a screen,
 * but its own dialog for a `Dialog` (a sheet, `onNode(isDialog())`), which the platform gives the back to first. The
 * dispatcher is the one the window's views name (`ViewTreeOnBackPressedDispatcherOwner`, set by a `ComponentDialog`
 * on its decor view as by an activity).
 */
fun SemanticsNodeInteraction.pressSystemBack() {
    val view = composeView()
    checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()) { "No back dispatcher above $view" }.pressSystemBack()
}

/**
 * The Back key, pressed and released, handed in this process to the window that shows [this] node, as its view root
 * hands it a key: for a `Popup` (a menu), which has no back dispatcher in its views, but takes Back as a key, and on
 * Android 13 and later as a callback of its window, out of reach of a test. Not injected through the system server, for
 * the reason given above. The window must be able to take keys (not `FLAG_NOT_FOCUSABLE`, as a `Popup` that is not
 * `focusable`): the platform would hand it no Back, which would go to the activity under it. This is checked on the
 * window itself, not on the focus of the moment, which another window may hold.
 */
fun SemanticsNodeInteraction.pressBackKey() {
    val window = composeView().rootView
    val flags = (window.layoutParams as WindowManager.LayoutParams).flags
    check(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE == 0) { "The window of $window takes no key" }
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
            window.dispatchKeyEvent(KeyEvent(action, KeyEvent.KEYCODE_BACK))
        }
    }
}

/** The view of the composition that shows [this] node, in its own window. */
private fun SemanticsNodeInteraction.composeView(): View =
    checkNotNull(fetchSemanticsNode().root as? ViewRootForTest) { "No compose view shows $this" }.view
