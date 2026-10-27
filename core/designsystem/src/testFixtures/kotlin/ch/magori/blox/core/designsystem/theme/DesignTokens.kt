package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * The tokens of docs/design-system/tokens.json, the copy of the published design system. The convention
 * `blox.design.tokens` gives its path to the tests in the system property `blox.designTokens` (ADR 0019).
 */
object DesignTokens {
    /** A token of the Colorful theme, as written for the day (light) and for the night (dark). */
    data class Written(val day: String, val night: String)

    /**
     * A text style, as written: the font of the family of its group (`Unbounded` for `display`), its size in px, its
     * line height in em and its weight.
     */
    data class WrittenText(val family: String, val fontSize: Float, val lineHeight: Float, val fontWeight: Int)

    private val root: JsonObject by lazy {
        val path = requireNotNull(System.getProperty("blox.designTokens")) {
            "Propriété blox.designTokens absente : appliquer le plugin blox.design.tokens au module."
        }
        Json.parseToJsonElement(File(path).readText()).jsonObject
    }

    /** Each colour token, by its name. */
    val colours: Map<String, Written> by lazy { themed("color") }

    /** Each shadow token, by its name. */
    val shadows: Map<String, Written> by lazy { themed("shadow") }

    /** Each token of a group of plain values (`radius`, `size`, `spacing`, `duration`), by its name, as written. */
    fun values(group: String): Map<String, String> =
        root.getValue(group).jsonObject.getValue("tokens").jsonArray.associate { token ->
            token.jsonObject.getValue("name").jsonPrimitive.content to
                token.jsonObject.getValue("value").jsonPrimitive.content
        }

    /** Each token of a [group] written for the day and for the night, by its name. */
    private fun themed(group: String): Map<String, Written> =
        root.getValue(group).jsonObject.getValue("tokens").jsonArray.associate { token ->
            val value = token.jsonObject.getValue("value").jsonObject
            token.jsonObject.getValue("name").jsonPrimitive.content to Written(
                day = value.getValue("light").jsonPrimitive.content,
                night = value.getValue("dark").jsonPrimitive.content,
            )
        }

    /** Each text style of the Colorful theme, by its name. */
    val textStyles: Map<String, WrittenText> by lazy {
        val type = root.getValue("type").jsonObject
        // A family is a CSS list, its font first: "\"Unbounded\", system-ui, sans-serif".
        val fonts = type.getValue("families").jsonObject.mapValues { (_, list) ->
            list.jsonPrimitive.content.substringBefore(',').trim('"')
        }
        type.getValue("groups").jsonArray.flatMap { group ->
            val family = fonts.getValue(group.jsonObject.getValue("family").jsonPrimitive.content)
            group.jsonObject.getValue("styles").jsonArray.map { style ->
                val written = style.jsonObject
                written.getValue("name").jsonPrimitive.content to WrittenText(
                    family = family,
                    fontSize = written.getValue("fontSize").jsonPrimitive.content.removeSuffix("px").toFloat(),
                    lineHeight = written.getValue("lineHeight").jsonPrimitive.float,
                    fontWeight = written.getValue("fontWeight").jsonPrimitive.int,
                )
            }
        }.toMap()
    }
}

private val PIXELS = Regex("""(-?\d+)px|0""")

/** A length as tokens json writes it, `16px` or `0`: 1 px of the design system is 1 dp of the code. */
fun cssDp(written: String): Dp {
    val pixels = requireNotNull(PIXELS.matchEntire(written)) { "Longueur CSS non lue : $written" }.groupValues[1]
    return pixels.ifEmpty { "0" }.toInt().dp
}

private val RGBA = Regex("""rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([\d.]+)\s*\)""")
private val HEX = Regex("""#([0-9A-Fa-f]{6})""")

/** A colour as tokens json writes it: `#RRGGBB`, opaque, or `rgba(r,g,b,a)`. */
fun cssColor(written: String): Color {
    HEX.matchEntire(written)?.let { hex -> return Color(0xFF000000 or hex.groupValues[1].toLong(radix = 16)) }
    val rgba = requireNotNull(RGBA.matchEntire(written)) { "Couleur CSS non lue : $written" }
    val (red, green, blue, alpha) = rgba.destructured
    return Color(red.toInt(), green.toInt(), blue.toInt()).copy(alpha = alpha.toFloat())
}

/** One shadow of a CSS `box-shadow`: inset or not, its four lengths (x, y, blur, spread), its colour. */
private val SHADOW = Regex("""(inset\s+)?(\S+)\s+(\S+)\s+(\S+)(?:\s+(\S+))?\s+(rgba\([^)]*\)|#[0-9A-Fa-f]{6})""")

/** The shadows of a `box-shadow`, split on the commas that are not in a colour. */
private val SHADOWS = Regex(""",\s*(?![^(]*\))""")

/**
 * A gloss as tokens json writes it, a CSS `box-shadow` without blur: each `inset 0 y 0 colour` is an inner shadow,
 * each `0 0 0 width colour` a ring, of a width above 0. Any other shadow cannot be drawn by the code, and is refused.
 */
fun cssGloss(written: String): Gloss = Gloss(
    written.trim().split(SHADOWS).map { shadow ->
        val (inset, lengths, colour) = cssShadow(shadow)
        when {
            inset.isNotEmpty() && lengths.withIndex().all { (index, length) -> index == 1 || length == 0.dp } ->
                GlossLayer.Inner(offsetY = lengths[1], color = cssColor(colour))

            inset.isEmpty() && lengths.take(3).all { it == 0.dp } && lengths[3] > 0.dp ->
                GlossLayer.Ring(width = lengths[3], color = cssColor(colour))

            else -> throw IllegalArgumentException("Ombre CSS que le code ne dessine pas : $shadow")
        }
    },
)

/**
 * A drop shadow as tokens json writes it, one CSS `box-shadow` `0 y blur colour`: neither inset, nor moved sideways,
 * nor spread, which the code does not draw, and refuses.
 */
fun cssDropShadow(written: String): DropShadow {
    val (inset, lengths, colour) = cssShadow(written)
    val (x, y, blur, spread) = lengths
    require(inset.isEmpty() && x == 0.dp && spread == 0.dp) { "Ombre portée que le code ne dessine pas : $written" }
    return DropShadow(offsetY = y, blur = blur, color = cssColor(colour))
}

/** One shadow of a CSS `box-shadow`: `inset` or nothing, its four lengths (x, y, blur, spread), its colour. */
private fun cssShadow(written: String): Triple<String, List<Dp>, String> {
    val match = requireNotNull(SHADOW.matchEntire(written.trim())) { "Ombre CSS non lue : $written" }
    val (inset, x, y, blur, spread, colour) = match.destructured
    return Triple(inset, listOf(x, y, blur, spread.ifEmpty { "0" }).map(::cssDp), colour)
}
