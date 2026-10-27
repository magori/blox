package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/** Puts the screen in the mode of a keyboard plugged in: a touch screen gives the focus to text fields only. */
@Composable
internal fun KeyboardInUse() {
    val inputModes = LocalInputModeManager.current
    LaunchedEffect(inputModes) { inputModes.requestInputMode(InputMode.Keyboard) }
}
