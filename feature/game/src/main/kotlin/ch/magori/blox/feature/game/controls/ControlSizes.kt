package ch.magori.blox.feature.game.controls

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces

/**
 * The sizes of the controls, in one place: 3 × 52 + 2 × 8 for ◀ ▼ ▶, 2 × 56 + 8 for B and A, which
 * fit a 360 dp wide phone inside its 24 dp margins (312 dp). Every button is at least
 * [BloxSizes.minTouchTarget], the accessibility minimum. The sizes of the keys are tokens of the design system, held
 * by [BloxSizes]: the compact ones for now, until the keys pick theirs by the width of the screen (P5.79).
 */
internal object ControlSizes {
    val directionButton = BloxSizes.keyArrowCompact
    val rotationButton = BloxSizes.keyRotateCompact
    val spacing = BloxSpaces.space2

    /** The arrow of rotation of B and A, above their letter. */
    val rotationArrow = 24.dp

    /** How far a held key goes down, in every theme: a cue other than its pressed fill. */
    val heldDrop = 2.dp

    /** B sits lower than A, as on a handheld console; ◀ ▼ ▶ half as low. */
    val rotationStagger = 24.dp

    /** The group ◀ ▼ ▶. */
    val directionsWidth = directionButton * 3 + spacing * 2
    val directionsHeight = directionButton + rotationStagger / 2

    /** The group B A. */
    val rotationsWidth = rotationButton * 2 + spacing
    val rotationsHeight = rotationButton + rotationStagger
}

/**
 * The height of the letter of B or A, [letter] high in its style: as high, or shrunk to hold under the arrow, within
 * the frame of the key, when the font of the system is enlarged.
 */
internal fun keyLetterHeight(letter: Dp): Dp =
    minOf(letter, ControlSizes.rotationButton - BloxBorders.frame * 2 - ControlSizes.rotationArrow)

/** How far the key goes down while it is [held]: [ControlSizes.heldDrop], or not at all. */
internal fun keyDrop(held: Boolean): Dp = if (held) ControlSizes.heldDrop else 0.dp
