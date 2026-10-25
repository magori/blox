package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
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
internal object DesignTokens {
    /** A colour of the Colorful theme, as written for the day (light) and for the night (dark). */
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
    val colours: Map<String, Written> by lazy {
        root.getValue("color").jsonObject.getValue("tokens").jsonArray.associate { token ->
            val value = token.jsonObject.getValue("value").jsonObject
            token.jsonObject.getValue("name").jsonPrimitive.content to Written(
                day = value.getValue("light").jsonPrimitive.content,
                night = value.getValue("dark").jsonPrimitive.content,
            )
        }
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

private val RGBA = Regex("""rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([\d.]+)\s*\)""")
private val HEX = Regex("""#([0-9A-Fa-f]{6})""")

/** A colour as tokens json writes it: `#RRGGBB`, opaque, or `rgba(r,g,b,a)`. */
internal fun cssColor(written: String): Color {
    HEX.matchEntire(written)?.let { hex -> return Color(0xFF000000 or hex.groupValues[1].toLong(radix = 16)) }
    val rgba = requireNotNull(RGBA.matchEntire(written)) { "Couleur CSS non lue : $written" }
    val (red, green, blue, alpha) = rgba.destructured
    return Color(red.toInt(), green.toInt(), blue.toInt()).copy(alpha = alpha.toFloat())
}
