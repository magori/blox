package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant

/**
 * A colour token of tokens json drawn by core:board in the Colorful theme only, and the [property] that holds it, read
 * in a variant: those of the padlock of the Locksmith (LockTokensTest) and of the musical garden (GardenTokensTest).
 */
class CodedColour(val name: String, private val property: String, val read: (BloxThemeVariant) -> Color) {
    override fun toString(): String = "$name → $property"
}
