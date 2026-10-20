package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScoreId
import kotlin.random.Random

/*
 * The rules of the names of the saved musics, the only source of them: the library keeps them (it refuses a name
 * taken), the list and the music editor show them (why a name is refused; the name the editor proposes, the free
 * one it offers). A name is always weighed without the spaces around it, and whatever its case; the music being
 * saved or renamed, known by its identity [ScoreId.Personal], never takes its own name.
 */

/** Whether one of these musics other than [apartFrom] is called [name], the case and the spaces around it ignored. */
fun List<SavedMusic>.isNameTaken(
    name: String,
    apartFrom: ScoreId.Personal? = null,
): Boolean = any { it.id != apartFrom && it.name.equals(name.trim(), ignoreCase = true) }

/** Why a saved music cannot take a name. */
enum class NameProblem {
    /** Empty, or only spaces. */
    BLANK,

    /** More characters than a saved name may have, [SavedMusic.MAX_NAME_LENGTH], the spaces around it apart. */
    TOO_LONG,

    /** Another music has it, the case and the spaces around it ignored ([isNameTaken]). */
    TAKEN,
}

/**
 * Why a music other than these, or [apartFrom] among them, cannot be called [name]: blank, too long, or taken
 * ([isNameTaken]), the spaces around it apart; null when it can. The rule of the list and of the editor.
 */
fun List<SavedMusic>.nameProblem(
    name: String,
    apartFrom: ScoreId.Personal? = null,
): NameProblem? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> NameProblem.BLANK
        !fitsInAName(trimmed) -> NameProblem.TOO_LONG
        isNameTaken(trimmed, apartFrom) -> NameProblem.TAKEN
        else -> null
    }
}

/**
 * The name proposed for a music of [style] on [scale], at [bpm] beats a minute (P4.27): one of the evocative names of
 * the style, or for a free music (null) of its mode and its tempo ([evocativeNames]), followed by its key
 * ([keyPhrase]), « Matin de fête en sol », drawn with [random] among those that no music has. Once all are taken, one of
 * them followed by the first number that makes it free ([freeName]). Never more than [SavedMusic.MAX_NAME_LENGTH]
 * characters.
 */
fun List<SavedMusic>.proposedName(
    style: Category?,
    scale: Scale,
    bpm: Int,
    random: Random,
): String = proposedNameAmong(evocativeNames(style, scale, bpm).map { "$it ${keyPhrase(scale)}" }, random)

/**
 * One of [names], drawn with [random], of [SavedMusic.MAX_NAME_LENGTH] characters at the most: one that no music has,
 * else one followed by the first number that makes it free, else, when none of them fits once numbered, one cut so that
 * its number fits ([cutFreeName]).
 */
internal fun List<SavedMusic>.proposedNameAmong(
    names: List<String>,
    random: Random,
): String {
    val free = names.filterNot(::isNameTaken).filter(::fitsInAName)
    val candidates = free.ifEmpty { names.map(::freeName).filter(::fitsInAName) }.ifEmpty { names.map(::cutFreeName) }
    return candidates.random(random)
}

/**
 * [wanted] cut at [SavedMusic.MAX_NAME_LENGTH] characters, without the spaces around it, when no music has it; else cut
 * shorter, so that the first number from 2 that makes it free still fits.
 */
private fun List<SavedMusic>.cutFreeName(wanted: String): String {
    val whole = cutName(wanted).trim()
    if (!isNameTaken(whole)) return whole
    return generateSequence(2) { it + 1 }
        .map { number -> " $number".let { cutTo(whole, SavedMusic.MAX_NAME_LENGTH - it.length).trimEnd() + it } }
        .first { !isNameTaken(it) }
}

/**
 * A name that no music other than [apartFrom] has, the one the editor offers to take (« Prendre « Joyeux 2 » »):
 * [wanted] as it is, without the spaces around it, when it is free; else [wanted] followed by the first number from 2
 * that makes it free. A [wanted] name that already ends with a number counts on from it: « Joyeux 3 » after
 * « Joyeux 2 »; a number too big to count on is part of the name.
 */
fun List<SavedMusic>.freeName(
    wanted: String,
    apartFrom: ScoreId.Personal? = null,
): String {
    val name = wanted.trim()
    if (!isNameTaken(name, apartFrom)) return name
    val number = ENDING_NUMBER.find(name)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it < Int.MAX_VALUE }
    return if (number == null) {
        numbered(name, from = 2, apartFrom)
    } else {
        numbered(name.substringBeforeLast(' '), from = number + 1, apartFrom)
    }
}

/** « [base] n », with the first number from [from] that no music other than [apartFrom] has. */
private fun List<SavedMusic>.numbered(
    base: String,
    from: Int,
    apartFrom: ScoreId.Personal?,
): String = generateSequence(from) { it + 1 }.map { "$base $it" }.first { !isNameTaken(it, apartFrom) }

/**
 * The name [typed] (in the editor, or in the list), cut at the most characters a saved name may have,
 * [SavedMusic.MAX_NAME_LENGTH], never between the two units of a character; a shorter name as it is, spaces kept.
 */
fun cutName(typed: String): String = cutTo(typed, SavedMusic.MAX_NAME_LENGTH)

/** [text] cut at [characters] characters, never between the two units of a character. */
private fun cutTo(
    text: String,
    characters: Int,
): String = text.take(text.offsetByCodePoints(0, minOf(text.codePointCount(0, text.length), characters)))

/** A number at the end of a name, after a space: « Joyeux 2 ». */
private val ENDING_NUMBER = Regex(""" ([0-9]+)$""")

/** Whether [name] has [SavedMusic.MAX_NAME_LENGTH] characters at the most. */
private fun fitsInAName(name: String): Boolean = name.codePointCount(0, name.length) <= SavedMusic.MAX_NAME_LENGTH
