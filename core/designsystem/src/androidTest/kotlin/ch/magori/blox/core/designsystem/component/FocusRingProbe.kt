package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import ch.magori.blox.core.designsystem.theme.BloxBorders
import de.mannodermaus.junit5.compose.ComposeContext
import kotlin.math.roundToInt

/**
 * The colour drawn where the ring of the keyboard focus of [node] passes, if it has one: the gap of the ring and half
 * its width off its left edge, halfway down. The node must stand that far from the left of the screen.
 */
internal fun ComposeContext.colorOnFocusRingOf(node: SemanticsNodeInteraction): Color {
    val bounds = node.fetchSemanticsNode().boundsInRoot
    val off = with(density) { (BloxBorders.focusGap + BloxBorders.focus / 2).toPx() }
    val image = onRoot().captureToImage().toPixelMap()
    return image[(bounds.left - off).roundToInt(), bounds.center.y.roundToInt()]
}
