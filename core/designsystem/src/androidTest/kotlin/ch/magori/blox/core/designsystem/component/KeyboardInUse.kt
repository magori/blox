package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Puts the screen in the mode of a keyboard plugged in: a touch screen gives the focus to text fields only. */
@Composable
internal fun KeyboardInUse() {
    val inputModes = LocalInputModeManager.current
    LaunchedEffect(inputModes) { inputModes.requestInputMode(InputMode.Keyboard) }
}

/** A place of 48 dp that may take the focus, and nothing else, tagged [tag]: where the focus may go next. */
@Composable
internal fun FocusTarget(tag: String) {
    Box(Modifier.size(48.dp).testTag(tag).focusable())
}
