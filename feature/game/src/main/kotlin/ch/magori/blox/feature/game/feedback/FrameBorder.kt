package ch.magori.blox.feature.game.feedback

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** The border of the frame of the board: [width], in [color]. */
@Immutable
internal data class FrameBorder(val width: Dp, val color: Color)

/** How thick the frame of the board is while the stack is in danger (docs/design-system/README.md, Retours de jeu). */
private val DangerFrame = 4.dp

/**
 * The border of the frame of the board in [palette]: [BloxBorders.frame] of `panel-border`, or [DangerFrame] of `danger`
 * while the stack is [inDanger].
 */
internal fun boardFrameBorder(
    palette: BloxPalette,
    inDanger: Boolean,
): FrameBorder =
    if (inDanger) FrameBorder(DangerFrame, palette.danger) else FrameBorder(BloxBorders.frame, palette.panelBorder)
