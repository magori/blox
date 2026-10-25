package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
        val match = requireNotNull(SHADOW.matchEntire(shadow.trim())) { "Ombre CSS non lue : $shadow" }
        val (inset, x, y, blur, spread, colour) = match.destructured
        val lengths = listOf(x, y, blur, spread.ifEmpty { "0" }).map(::cssDp)
        when {
            inset.isNotEmpty() && lengths.withIndex().all { (index, length) -> index == 1 || length == 0.dp } ->
                GlossLayer.Inner(offsetY = lengths[1], color = cssColor(colour))

            inset.isEmpty() && lengths.take(3).all { it == 0.dp } && lengths[3] > 0.dp ->
                GlossLayer.Ring(width = lengths[3], color = cssColor(colour))

            else -> throw IllegalArgumentException("Ombre CSS que le code ne dessine pas : $shadow")
        }
    },
)
