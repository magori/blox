package ch.magori.blox.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.R

/**
 * Jetons de typographie de l'interface : la police et la graisse de chaque rôle de texte, sans taille (les
 * écrans gardent les leurs). Les polices sont embarquées dans res/font, voir docs/themes.md.
 */
@Immutable
data class BloxTypography(
    /** Titres : accueil, écrans (pause, fin de partie), barre du haut, badge. */
    val title: TextStyle,
    /** Libellés des encarts, choix, boutons. */
    val label: TextStyle,
    /** Valeurs chiffrées : score, niveau, lignes, record. */
    val value: TextStyle,
    /** La largeur de chaque chiffre de la police de [value]. */
    private val valueDigits: DigitWidths = DigitWidths.Fixed,
) {
    /**
     * [text] tel qu'une valeur l'affiche : chaque chiffre aussi large que le plus large, pour qu'une valeur ne
     * bouge pas quand elle change. Les polices sans chiffres tabulaires (ni `tnum`) sont complétées par un
     * espacement en em, que la taille automatique suit.
     */
    fun valueText(text: String): AnnotatedString = valueDigits.pad(text)
}

/**
 * La typographie Material du thème, pour ce qui la lit (accueil, boutons) : les tailles de Material, la police
 * et la graisse des titres pour les styles display, headline et title, celles des libellés pour les autres.
 */
internal fun BloxTypography.toMaterial(): Typography {
    val base = Typography()
    fun TextStyle.titled() = merge(title)
    fun TextStyle.labelled() = merge(label)
    return base.copy(
        displayLarge = base.displayLarge.titled(),
        displayMedium = base.displayMedium.titled(),
        displaySmall = base.displaySmall.titled(),
        headlineLarge = base.headlineLarge.titled(),
        headlineMedium = base.headlineMedium.titled(),
        headlineSmall = base.headlineSmall.titled(),
        titleLarge = base.titleLarge.titled(),
        titleMedium = base.titleMedium.titled(),
        titleSmall = base.titleSmall.titled(),
        bodyLarge = base.bodyLarge.labelled(),
        bodyMedium = base.bodyMedium.labelled(),
        bodySmall = base.bodySmall.labelled(),
        labelLarge = base.labelLarge.labelled(),
        labelMedium = base.labelMedium.labelled(),
        labelSmall = base.labelSmall.labelled(),
    )
}

/**
 * La largeur, en em, des chiffres 0 à 9 d'une police, telle que sa table hmtx la donne. Une police dont les
 * chiffres sont proportionnels les complète par un espacement : Android le répartit de part et d'autre du
 * glyphe, qui reste centré dans sa case, comme un chiffre tabulaire.
 */
@Immutable
class DigitWidths(private val ems: List<Float>) {
    init {
        require(ems.size == DIGIT_COUNT) { "One width per digit, 0 to 9" }
    }

    fun pad(text: String): AnnotatedString = buildAnnotatedString {
        val widest = ems.max()
        text.forEach { char ->
            val padding = if (char.isAsciiDigit()) widest - ems[char - '0'] else 0f
            if (padding > 0f) withStyle(SpanStyle(letterSpacing = padding.em)) { append(char) } else append(char)
        }
    }

    companion object {
        private const val DIGIT_COUNT = 10

        /** Une police à chasse fixe : rien à compléter. */
        val Fixed = DigitWidths(List(DIGIT_COUNT) { 1f })
    }
}

private fun Char.isAsciiDigit() = this in '0'..'9'

private val PressStart2P = FontFamily(Font(R.font.press_start_2p_regular, FontWeight.Normal))

private val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_regular, FontWeight.Normal),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)

/** Le style d'une valeur : sans crénage, qui rapprocherait deux chiffres et ferait bouger la valeur. */
private fun valueStyle(text: TextStyle) = text.copy(fontFeatureSettings = "\"kern\" 0")

object BloxTypographies {
    /** Thème « Original » : tout en police pixel, à sa seule graisse (un gras synthétisé la brouillerait). */
    val Original = TextStyle(fontFamily = PressStart2P, fontWeight = FontWeight.Normal).let { pixel ->
        BloxTypography(title = pixel, label = pixel, value = valueStyle(pixel))
    }

    /** Thème « Couleurs » : Chakra Petch, en gras pour les titres et les valeurs. */
    val Colorful = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold).let { bold ->
        BloxTypography(
            title = bold,
            label = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Normal),
            value = valueStyle(bold),
            // Chakra Petch Bold, hmtx sur 1000 unités par em ; ses chiffres sont proportionnels, sans tnum.
            valueDigits = DigitWidths(
                listOf(0.652f, 0.382f, 0.574f, 0.603f, 0.579f, 0.606f, 0.622f, 0.522f, 0.638f, 0.627f),
            ),
        )
    }

    /**
     * La plus petite taille à laquelle un texte d'une ligne réduit le sien pour tenir dans sa place, pour rester
     * lisible. La police pixel prend 1 em par caractère : dans les 92 dp d'un encart, 8 sp tiennent 11 caractères
     * à la taille de police normale, mais 5 seulement avec la police du système agrandie de 200 % (16 dp chacun).
     */
    val MinimumFontSize = 8.sp

    fun of(kind: BloxThemeKind): BloxTypography = when (kind) {
        BloxThemeKind.ORIGINAL -> Original
        BloxThemeKind.COLORFUL -> Colorful
    }
}
