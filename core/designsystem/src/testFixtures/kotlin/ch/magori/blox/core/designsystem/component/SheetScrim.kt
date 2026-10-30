package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp

/**
 * Touches the scrim of the sheet open, [this] being its dialog (`onNode(isDialog())`), as a player who closes it does:
 * near the top of the screen, above the panel, which stands 48 dp at least under the top, whatever it holds.
 */
fun SemanticsNodeInteraction.tapScrim(): SemanticsNodeInteraction =
    performTouchInput { click(Offset(centerX, SCRIM_TAP.toPx())) }

/** How far from the top of the screen the scrim is touched: half the room it keeps above a sheet. */
private val SCRIM_TAP = 24.dp
