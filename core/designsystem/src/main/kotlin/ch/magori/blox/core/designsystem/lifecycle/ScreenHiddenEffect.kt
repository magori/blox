package ch.magori.blox.core.designsystem.lifecycle

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Calls [onHidden] each time the screen is no longer visible (`ON_STOP`: the application in the background, or another
 * screen in front): the screens that listen to music stop it there. A pause alone, behind a dialog or in a split
 * screen, does nothing: the music plays on.
 */
@Composable
fun ScreenHiddenEffect(onHidden: () -> Unit) = LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = onHidden)
