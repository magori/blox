package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Jetons de couleur de l'interface (hors pièces). Les valeurs viennent de la maquette,
 * voir docs/themes.md. Les couleurs des pièces sont associées aux types de pièces
 * dans feature:game, pour que le design system ne dépende pas du domaine.
 */
@Immutable
data class BloxPalette(
    val page: Color,
    val ink: Color,
    val label: Color,
    val panel: Color,
    val panelBorder: Color,
    val frame: Color,
    val boardBackground: Color,
    val button: Color,
    val buttonInk: Color,
    val accent: Color,
    val accentInk: Color,
    /** The four cells of the logo of the home screen. */
    val logoCells: LogoColors,
)

object BloxPalettes {
    /** Thème « Original » : vert sauge pastel, inspiré des écrans monochromes. */
    val Original = BloxPalette(
        page = Color(0xFFCFE3C8),
        ink = Color(0xFF34503A),
        label = Color(0xFF34503A),
        panel = Color(0xFFBFD6B6),
        panelBorder = Color(0xFF34503A),
        frame = Color(0xFF34503A),
        boardBackground = Color(0xFFDCEBD5),
        button = Color(0xFF5F7F5C),
        buttonInk = Color(0xFFEEF5EA),
        accent = Color(0xFF34503A),
        accentInk = Color(0xFFEEF5EA),
        // The four greens of the pieces, the fills of I, O, J and T: dark, medium, light, pale.
        logoCells = LogoColors(
            topLeft = Color(0xFF34503A),
            topRight = Color(0xFF87A67F),
            bottomLeft = Color(0xFFBFD6B6),
            bottomRight = Color(0xFFEEF5EA),
        ),
    )

    /** Thème « Couleurs » : fond sombre, une couleur vive par pièce. */
    val Colorful = BloxPalette(
        page = Color(0xFF12141C),
        ink = Color(0xFFF1F1F4),
        label = Color(0xFF9AA0B4),
        panel = Color(0xFF1C1F2B),
        panelBorder = Color(0xFF2A2E3E),
        frame = Color(0xFF2A2E3E),
        boardBackground = Color(0xFF0B0C12),
        button = Color(0xFF2A2E3E),
        buttonInk = Color(0xFFF1F1F4),
        accent = Color(0xFFEF8D3B),
        accentInk = Color(0xFF12141C),
        // The fills of the I, O, Z and S pieces.
        logoCells = LogoColors(
            topLeft = Color(0xFF3FC5E8),
            topRight = Color(0xFFF2C641),
            bottomLeft = Color(0xFFE5534B),
            bottomRight = Color(0xFF5BC46A),
        ),
    )
}
