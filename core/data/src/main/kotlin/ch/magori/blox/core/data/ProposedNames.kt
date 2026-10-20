package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType

/**
 * The key of this scale in French, its tonic ([tonicName]) and its mode: « sol majeur » for a major scale, the
 * pentatonic one included, « ré mineur » for a minor one, natural or harmonic, « ré dorien », « sol mixolydien ».
 */
fun Scale.keyName(): String = "${tonicName()} ${modeName()}"

/**
 * The tonic of this scale in French, « sol », « si bémol », « fa dièse », spelt as the key signature of its scale
 * writes it: the one of the major scale it is a degree of (its relative major for a minor scale), with the fewest
 * sharps or flats, sharps on a tie (fa dièse majeur rather than sol bémol majeur).
 */
fun Scale.tonicName(): String {
    val tonic = tonic.midi.mod(SEMITONES)
    val degree = when (type) {
        ScaleType.MAJOR, ScaleType.MAJOR_PENTATONIC -> 0
        ScaleType.DORIAN -> 1
        ScaleType.MIXOLYDIAN -> 4
        ScaleType.NATURAL_MINOR, ScaleType.HARMONIC_MINOR -> 5
    }
    val major = (tonic - MAJOR_STEPS[degree]).mod(SEMITONES)
    val letter = (MAJOR_LETTERS[major] + degree) % LETTERS.size
    val accidental = when ((tonic - MAJOR_STEPS[letter]).mod(SEMITONES)) {
        0 -> null

        1 -> "dièse"

        // Only 11, a semitone under the letter: the major scales spelt here have six sharps or flats at the most, so
        // none of the degrees named needs two of them (every tonic of every scale is checked by ProposedNamesTest).
        else -> "bémol"
    }
    return listOfNotNull(LETTERS[letter], accidental).joinToString(" ")
}

/**
 * The key of [scale] as a name says it, « en » and its key ([keyName]), « majeur » left out: « en sol »,
 * « en ré mineur », « en ré dorien ».
 */
internal fun keyPhrase(scale: Scale): String =
    if (scale.modeName() == MAJOR) "en ${scale.tonicName()}" else "en ${scale.keyName()}"

/** The mode of this scale as its key says it: « majeur », « mineur », « dorien », « mixolydien ». */
private fun Scale.modeName(): String = when (type) {
    ScaleType.MAJOR, ScaleType.MAJOR_PENTATONIC -> MAJOR
    ScaleType.NATURAL_MINOR, ScaleType.HARMONIC_MINOR -> "mineur"
    ScaleType.DORIAN -> "dorien"
    ScaleType.MIXOLYDIAN -> "mixolydien"
}

private const val MAJOR = "majeur"

private const val SEMITONES = 12

/** The names of the notes without sharp nor flat, from do. */
private val LETTERS = listOf("do", "ré", "mi", "fa", "sol", "la", "si")

/** Semitones from the tonic of a major scale to each of its degrees: from do, those of the notes of [LETTERS]. */
private val MAJOR_STEPS = listOf(0, 2, 4, 5, 7, 9, 11)

/**
 * The letter of the tonic of each major scale, by its semitones above do: do, ré bémol, ré, mi bémol, mi, fa,
 * fa dièse, sol, la bémol, la, si bémol, si.
 */
private val MAJOR_LETTERS = listOf(0, 1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6)

/**
 * The evocative names of each style, without their key: 17 characters at the most, so that the longest key,
 * « en fa dièse mixolydien », still fits in [SavedMusic.MAX_NAME_LENGTH].
 */
private val STYLE_NAMES: Map<Category, List<String>> = mapOf(
    Category.JOYFUL to listOf("Matin de fête", "Jour de marché", "Ronde des cerises", "Soleil levant"),
    Category.CALM to listOf("Brume du lac", "Jardin du soir", "Sieste au verger", "Ciel de lait"),
    Category.EPIC to listOf("Bannière au vent", "Les remparts", "Chevauchée", "Marche des géants"),
    Category.MELANCHOLIC to listOf("Pluie de novembre", "Quai désert", "Dernier train", "Lettre perdue"),
    Category.MYSTERIOUS to listOf("Couloir secret", "Ombre du phare", "Carte effacée", "Lanterne au bois"),
    Category.RETRO to listOf("Niveau bonus", "Salle d’arcade", "Écran titre", "Dernière vie"),
    Category.DANCE to listOf("Bal du port", "Tourbillon", "Nuit de fanfare", "Pas chassé"),
)

/**
 * The names a music of [style] may be given, without their key: those of its style, or, for a free music (null), those
 * of its mode and its tempo: minor for a minor or dorian [scale], major for the others, slow under [FAST_BPM] beats a
 * minute.
 */
internal fun evocativeNames(
    style: Category?,
    scale: Scale,
    bpm: Int,
): List<String> {
    if (style != null) return STYLE_NAMES.getValue(style)
    val minor = scale.type in MINOR_TYPES
    return when {
        bpm < FAST_BPM -> if (minor) SLOW_MINOR_NAMES else SLOW_MAJOR_NAMES
        else -> if (minor) FAST_MINOR_NAMES else FAST_MAJOR_NAMES
    }
}

/** The scales a free music is named minor in: the dorian one too. */
private val MINOR_TYPES = setOf(ScaleType.NATURAL_MINOR, ScaleType.HARMONIC_MINOR, ScaleType.DORIAN)

/** The tempo from which a free music is named fast. */
private const val FAST_BPM = 100

/** The names of a free music, by its mode and its tempo. */
private val SLOW_MINOR_NAMES = listOf("Rêverie", "Songe", "Clair-obscur")
private val SLOW_MAJOR_NAMES = listOf("Promenade", "Berceuse", "Flânerie")
private val FAST_MINOR_NAMES = listOf("Poursuite", "Orage", "Fuite")
private val FAST_MAJOR_NAMES = listOf("Envolée", "Galop", "Cavalcade")
