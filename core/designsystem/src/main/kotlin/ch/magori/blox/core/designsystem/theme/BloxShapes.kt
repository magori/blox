package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Jetons de forme de l'interface. Les valeurs viennent de la maquette, voir docs/themes.md. */
@Immutable
data class BloxShapes(
    /** Encarts (score, niveau…) : fond et cadre. */
    val panel: Shape,
    /** Boutons : commandes, boutons des écrans (Reprendre, Menu, Rejouer), bouton pause. */
    val button: Shape,
)

object BloxShapeSets {
    /** Thème « Original » : coins carrés. */
    val Original = BloxShapes(panel = RectangleShape, button = RectangleShape)

    /** Thème « Couleurs » : coins arrondis de 10 points. */
    val Colorful = BloxShapes(panel = RoundedCornerShape(10.dp), button = RoundedCornerShape(10.dp))
}

/** Épaisseurs de trait de l'interface, les mêmes dans les deux thèmes. */
object BloxBorders {
    /** Cadre d'un encart, d'un bouton secondaire et d'un choix de l'accueil. */
    val frame = 2.dp
}
