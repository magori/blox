package ch.magori.blox.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The frame of the board (views V3, V28), drawn under its cells: a block of `board` framed by 2 dp of `panel-border`,
 * corners `radius-md` (square in Original). Its size, around the cells, comes from [GameLayout.boardFrame].
 */
@Composable
internal fun BoardFrame(modifier: Modifier = Modifier) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    Spacer(
        modifier = modifier
            .background(palette.boardBackground, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape),
    )
}
