package ch.magori.blox.core.designsystem.theme

import java.io.File
import java.nio.ByteBuffer

/**
 * The few tables of a TrueType font file that tell how wide its digits are, read as the OpenType specification
 * writes them: the glyph of a character (cmap, format 4), its advance (hmtx), the glyph that a feature such as
 * `tnum` puts in its place (GSUB, single substitution) and how the advance varies along the axes of a variable
 * font (HVAR). java.awt reads neither GSUB features nor variations, which is why the tests read the file itself.
 */
internal class OpenTypeFont(file: File) {
    private val bytes = ByteBuffer.wrap(file.readBytes())

    private val tables: Map<String, Int> = (0 until u16(NUM_TABLES)).associate { index ->
        val record = TABLE_RECORDS + TABLE_RECORD_SIZE * index
        tag(record) to u32(record + TABLE_OFFSET)
    }

    /** Whether the font varies along axes, as a variable font (fvar). */
    val isVariable: Boolean = "fvar" in tables

    /** The units per em, in which [advance] is written. */
    val unitsPerEm: Int = u16(table("head") + HEAD_UNITS_PER_EM)

    /**
     * The glyph that draws [char], from the Unicode BMP subtable of cmap (platform 3, encoding 1, format 4): 0, the
     * missing glyph, for a character that the font does not draw.
     */
    fun glyph(char: Char): Int {
        val cmap = table("cmap")
        val subtable = (0 until u16(cmap + 2)).map { cmap + 4 + 8 * it }
            .first { u16(it) == WINDOWS && u16(it + 2) == UNICODE_BMP && u16(cmap + u32(it + 4)) == 4 }
            .let { cmap + u32(it + 4) }
        val segments = u16(subtable + 6) / 2
        val ends = subtable + 14
        val starts = ends + 2 * segments + 2
        val deltas = starts + 2 * segments
        val rangeOffsets = deltas + 2 * segments
        val code = char.code
        val segment = (0 until segments).first { code <= u16(ends + 2 * it) }
        if (code < u16(starts + 2 * segment)) return MISSING
        val delta = u16(deltas + 2 * segment)
        val rangeOffset = u16(rangeOffsets + 2 * segment)
        if (rangeOffset == 0) return (code + delta) and 0xFFFF
        val glyph = u16(rangeOffsets + 2 * segment + rangeOffset + 2 * (code - u16(starts + 2 * segment)))
        // A 0 in glyphIdArray is the missing glyph, to which idDelta does not apply.
        return if (glyph == MISSING) MISSING else (glyph + delta) and 0xFFFF
    }

    /** The advance of [glyph] at the default instance, in [unitsPerEm]. */
    fun advance(glyph: Int): Int {
        val longMetrics = u16(table("hhea") + HHEA_NUMBER_OF_H_METRICS)
        return u16(table("hmtx") + 4 * minOf(glyph, longMetrics - 1))
    }

    /** The features of GSUB, by their tag. */
    val features: Set<String> by lazy { featureRecords().map { it.first }.toSet() }

    /** The glyph that the single substitutions of [feature] (GSUB lookup type 1) put in the place of [glyph]. */
    fun substitute(
        feature: String,
        glyph: Int,
    ): Int {
        val gsub = table("GSUB")
        val lookups = gsub + u16(gsub + 8)
        return featureRecords().filter { it.first == feature }.flatMap { (_, offset) ->
            (0 until u16(offset + 2)).map { u16(offset + 4 + 2 * it) }
        }.fold(glyph) { current, lookupIndex ->
            val lookup = lookups + u16(lookups + 2 + 2 * lookupIndex)
            (0 until u16(lookup + 4)).map { lookup + u16(lookup + 6 + 2 * it) }.fold(current) { inner, start ->
                singleSubstitution(u16(lookup), start, inner)
            }
        }
    }

    /**
     * How the advance of [glyph] varies with each region of the axes of a variable font (HVAR), in [unitsPerEm], by
     * index of the region in the region list of the font, a region without delta left out: two glyphs of the same
     * advance and the same variations have the same advance at every weight, whatever subtable (ItemVariationData)
     * holds them and in whatever order it lists its regions. Empty for a font that does not vary.
     */
    fun advanceVariations(glyph: Int): Map<Int, Int> {
        val hvar = tables["HVAR"]
        if (hvar == null) {
            check("fvar" !in tables) { "fvar without HVAR" }
            return emptyMap()
        }
        val store = hvar + u32(hvar + 4)
        val (outer, inner) = deltaSetIndex(hvar, glyph)
        val data = store + u32(store + 8 + 4 * outer)
        val wordCount = u16(data + 2)
        check(wordCount and LONG_WORDS == 0) { "Deltas on 32 bits are not read" }
        val regions = u16(data + 4)
        var offset = data + 6 + 2 * regions + inner * (2 * wordCount + (regions - wordCount))
        val deltas = (0 until regions).map { region ->
            if (region < wordCount) bytes.getShort(offset).toInt().also { offset += 2 } else bytes.get(offset++).toInt()
        }
        return (0 until regions).associate { u16(data + 6 + 2 * it) to deltas[it] }.filterValues { it != 0 }
    }

    private fun deltaSetIndex(
        hvar: Int,
        glyph: Int,
    ): Pair<Int, Int> {
        val mapOffset = u32(hvar + 8)
        if (mapOffset == 0) return 0 to glyph
        val map = hvar + mapOffset
        val format = u8(map)
        val entryFormat = u8(map + 1)
        val count = if (format == 0) u16(map + 2) else u32(map + 2)
        val entries = if (format == 0) map + 4 else map + 6
        val size = ((entryFormat shr 4) and 3) + 1
        val innerBits = (entryFormat and 0xF) + 1
        val start = entries + minOf(glyph, count - 1) * size
        val entry = (0 until size).fold(0) { value, byte -> (value shl 8) or u8(start + byte) }
        return (entry shr innerBits) to (entry and ((1 shl innerBits) - 1))
    }

    private fun singleSubstitution(
        type: Int,
        start: Int,
        glyph: Int,
    ): Int = when (type) {
        EXTENSION -> singleSubstitution(u16(start + 2), start + u32(start + 4), glyph)

        SINGLE -> {
            val coverage = coverageIndex(start + u16(start + 2), glyph)
            when {
                coverage < 0 -> glyph
                u16(start) == 1 -> (glyph + bytes.getShort(start + 4)) and 0xFFFF
                else -> u16(start + 6 + 2 * coverage)
            }
        }

        else -> error("Lookup type $type is not a single substitution")
    }

    private fun coverageIndex(
        coverage: Int,
        glyph: Int,
    ): Int {
        val count = u16(coverage + 2)
        return if (u16(coverage) == 1) {
            (0 until count).firstOrNull { u16(coverage + 4 + 2 * it) == glyph } ?: -1
        } else {
            (0 until count).map { coverage + 4 + 6 * it }.firstOrNull { glyph in u16(it)..u16(it + 2) }
                ?.let { range -> u16(range + 4) + glyph - u16(range) } ?: -1
        }
    }

    /** Each feature of GSUB: its tag and where its table starts. */
    private fun featureRecords(): List<Pair<String, Int>> {
        val gsub = table("GSUB")
        val list = gsub + u16(gsub + 6)
        return (0 until u16(list)).map { list + 2 + 6 * it }.map { tag(it) to list + u16(it + 4) }
    }

    private fun table(tag: String): Int = checkNotNull(tables[tag]) { "No $tag table" }

    private fun u8(offset: Int): Int = bytes.get(offset).toInt() and 0xFF

    private fun u16(offset: Int): Int = bytes.getShort(offset).toInt() and 0xFFFF

    private fun u32(offset: Int): Int = bytes.getInt(offset)

    private fun tag(offset: Int): String = String(ByteArray(4) { bytes.get(offset + it) }, Charsets.US_ASCII)

    private companion object {
        const val NUM_TABLES = 4
        const val TABLE_RECORDS = 12
        const val TABLE_RECORD_SIZE = 16
        const val TABLE_OFFSET = 8
        const val HEAD_UNITS_PER_EM = 18
        const val HHEA_NUMBER_OF_H_METRICS = 34
        const val WINDOWS = 3
        const val UNICODE_BMP = 1
        const val SINGLE = 1
        const val EXTENSION = 7
        const val LONG_WORDS = 0x8000
        const val MISSING = 0
    }
}

/** The embedded font file [name] of res/font, without its extension, whose folder the build gives in blox.fontDir. */
internal fun embeddedFont(name: String): OpenTypeFont {
    val dir = checkNotNull(System.getProperty("blox.fontDir")) { "The build gives res/font in blox.fontDir" }
    return OpenTypeFont(File(dir, "$name.ttf"))
}
