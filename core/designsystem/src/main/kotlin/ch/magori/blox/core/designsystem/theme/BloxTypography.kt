package ch.magori.blox.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.R

/**
 * Jetons de typographie de l'interface. Les rôles [title], [label] et [value] portent la police et la graisse, sans
 * taille (les écrans gardent les leurs) ; les styles de texte du design system ([screenTitle] à [logo], ceux de
 * docs/design-system/tokens.json) portent aussi la taille et l'interligne en Couleurs, la police seule en Original,
 * qui n'a pas encore ses tailles. Les polices sont embarquées dans res/font, voir docs/themes.md.
 */
@Immutable
data class BloxTypography(
    /** Titres : accueil, écrans (pause, fin de partie), barre du haut, badge. */
    val title: TextStyle,
    /** Libellés des encarts, choix, boutons. */
    val label: TextStyle,
    /** Valeurs chiffrées : score, niveau, lignes, record. */
    val value: TextStyle,
    /** `screen-title` : titre d'un écran plein (Pause, Partie terminée, Paramètres). */
    val screenTitle: TextStyle,
    /** `final-score` : score de fin de partie, chiffres à chasse fixe. */
    val finalScore: TextStyle,
    /** `record` : record en haut de l'écran de jeu et à l'accueil, chiffres à chasse fixe. */
    val record: TextStyle,
    /** `stat-value` : valeur d'un encart (score, niveau, lignes), chiffres à chasse fixe. */
    val statValue: TextStyle,
    /** `game-title` : titre de la barre du haut, en capitales. */
    val gameTitle: TextStyle,
    /** `stat-label` : étiquette d'encart, en capitales. */
    val statLabel: TextStyle,
    /** `button-label` : libellé des boutons secondaires (Menu, Changer, tuiles). */
    val buttonLabel: TextStyle,
    /** `choice` : choix de l'accueil et lignes des Paramètres. */
    val choice: TextStyle,
    /** `section` : titre de section. */
    val section: TextStyle,
    /** `small` : note sous une section, nom de fichier, actions discrètes. */
    val small: TextStyle,
    /** `play-label` : libellé du bouton Jouer. */
    val playLabel: TextStyle,
    /** `four-lines-pop` : autocollant d'un effacement de 4 lignes. */
    val fourLinesPop: TextStyle,
    /** `points-pop` : autocollant des points gagnés, et NOUVEAU RECORD. */
    val pointsPop: TextStyle,
    /** `button-label-strong` : libellé des boutons principaux (Reprendre, Rejouer). */
    val buttonLabelStrong: TextStyle,
    /** `logo` : le titre BloX de l'accueil, et lui seul. */
    val logo: TextStyle,
)

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

private val PressStart2P = FontFamily(Font(R.font.press_start_2p_regular, FontWeight.Normal))

/**
 * L'instance [weight] de la police variable [resId] : sans réglage de son axe wght, Android dessinerait l'instance
 * par défaut du fichier (ExtraLight pour Manrope) et en synthétiserait le gras (API 26 et plus, `minSdk` 26).
 */
private fun variableFont(
    resId: Int,
    weight: FontWeight,
) = Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/**
 * Manrope et Unbounded sont publiées en police variable, sur l'axe wght : chaque graisse en est une instance, posée
 * sur l'axe par [variableFont]. Seules les graisses du design system y sont : une autre serait synthétisée.
 */
private val Manrope = FontFamily(
    variableFont(R.font.manrope_variable, FontWeight.Normal),
    variableFont(R.font.manrope_variable, FontWeight.Medium),
    variableFont(R.font.manrope_variable, FontWeight.Bold),
    variableFont(R.font.manrope_variable, FontWeight.ExtraBold),
)

private val Unbounded = FontFamily(
    variableFont(R.font.unbounded_variable, FontWeight.Bold),
    variableFont(R.font.unbounded_variable, FontWeight.ExtraBold),
)

private val BagelFatOne = FontFamily(Font(R.font.bagel_fat_one_regular, FontWeight.Normal))

/**
 * Le style d'une valeur : sans crénage, qui rapprocherait deux chiffres et ferait bouger la valeur. Avec [tabular], les
 * chiffres tabulaires de la police (`tnum`), tous de la même largeur.
 */
private fun valueStyle(
    text: TextStyle,
    tabular: Boolean = false,
) = text.copy(fontFeatureSettings = if (tabular) "\"kern\" 0, \"tnum\"" else "\"kern\" 0")

/** Un style de texte de tokens.json : 1 px du design system vaut 1 sp, l'interligne est en em. */
private fun textStyle(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Float,
) = TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.em)

object BloxTypographies {
    /**
     * Thème « Original » : tout en police pixel, à sa seule graisse (un gras synthétisé la brouillerait). Les styles du
     * design system n'y ont que la police : les écrans d'Original gardent leurs tailles.
     */
    val Original = TextStyle(fontFamily = PressStart2P, fontWeight = FontWeight.Normal).let { pixel ->
        val digits = valueStyle(pixel)
        BloxTypography(
            title = pixel,
            label = pixel,
            value = digits,
            screenTitle = pixel,
            finalScore = digits,
            record = digits,
            statValue = digits,
            gameTitle = pixel,
            statLabel = pixel,
            buttonLabel = pixel,
            choice = pixel,
            section = pixel,
            small = pixel,
            playLabel = pixel,
            fourLinesPop = pixel,
            pointsPop = pixel,
            buttonLabelStrong = pixel,
            logo = pixel,
        )
    }

    /**
     * Thème « Couleurs » (ADR 0021) : Unbounded pour les titres d'écran, Jouer et les autocollants, Bagel Fat One pour
     * le seul logo, Manrope pour le reste ; les chiffres prennent les chiffres tabulaires de Manrope.
     */
    val Colorful = BloxTypography(
        title = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
        label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium),
        value = valueStyle(TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold), tabular = true),
        screenTitle = textStyle(Unbounded, FontWeight.Bold, size = 22, lineHeight = 1.4f),
        finalScore = valueStyle(textStyle(Manrope, FontWeight.Bold, size = 40, lineHeight = 1.2f), tabular = true),
        record = valueStyle(textStyle(Manrope, FontWeight.Bold, size = 17, lineHeight = 1.2f), tabular = true),
        statValue = valueStyle(textStyle(Manrope, FontWeight.Bold, size = 17, lineHeight = 1.3f), tabular = true),
        gameTitle = textStyle(Manrope, FontWeight.Bold, size = 15, lineHeight = 1.2f),
        statLabel = textStyle(Manrope, FontWeight.Medium, size = 11, lineHeight = 1.6f),
        buttonLabel = textStyle(Manrope, FontWeight.Bold, size = 15, lineHeight = 1.2f),
        choice = textStyle(Manrope, FontWeight.Medium, size = 15, lineHeight = 1.2f),
        section = textStyle(Manrope, FontWeight.Normal, size = 15, lineHeight = 1.4f),
        small = textStyle(Manrope, FontWeight.Normal, size = 14, lineHeight = 1.6f),
        playLabel = textStyle(Unbounded, FontWeight.ExtraBold, size = 24, lineHeight = 1.1f),
        fourLinesPop = textStyle(Unbounded, FontWeight.ExtraBold, size = 22, lineHeight = 1.1f),
        pointsPop = textStyle(Unbounded, FontWeight.ExtraBold, size = 15, lineHeight = 1.1f),
        buttonLabelStrong = textStyle(Manrope, FontWeight.ExtraBold, size = 16, lineHeight = 1.2f),
        logo = textStyle(BagelFatOne, FontWeight.Normal, size = 56, lineHeight = 0.9f),
    )

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
