package ch.magori.blox.feature.game.controls

import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes

/**
 * The sizes of the controls, in one place: 3 × 52 + 2 × 8 for ◀ ▼ ▶, 2 × 56 + 8 for B and A, which
 * fit a 360 dp wide phone inside its 24 dp margins (312 dp). Every button is at least
 * [BloxSizes.minTouchTarget], the accessibility minimum.
 */
internal object ControlSizes {
    val directionButton = BloxSizes.minTouchTarget + 4.dp
    val rotationButton = BloxSizes.minTouchTarget + 8.dp
    val spacing = 8.dp

    /** B sits lower than A, as on a handheld console; ◀ ▼ ▶ half as low. */
    val rotationStagger = 24.dp

    /** The group ◀ ▼ ▶. */
    val directionsWidth = directionButton * 3 + spacing * 2
    val directionsHeight = directionButton + rotationStagger / 2

    /** The group B A. */
    val rotationsWidth = rotationButton * 2 + spacing
    val rotationsHeight = rotationButton + rotationStagger
}
