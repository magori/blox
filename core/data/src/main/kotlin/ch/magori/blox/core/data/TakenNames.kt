package ch.magori.blox.core.data

import kotlin.random.Random

/**
 * The names a list already has, each by the identity [Id] of what it names (a saved music, a kept garden), and the
 * rules a new name follows among them. A name is always weighed without the spaces around it, and whatever its case.
 * [names] holds one name by identity: given twice the same identity, a map keeps only its last name.
 */
class TakenNames<Id : Any>(private val names: Map<Id, String>) {
    /** Whether something of the list other than [apartFrom] is called [name], the case and the spaces around it ignored. */
    fun isTaken(
        name: String,
        apartFrom: Id? = null,
    ): Boolean {
        val trimmed = name.trim()
        return names.any { (id, taken) -> id != apartFrom && taken.equals(trimmed, ignoreCase = true) }
    }

    /** Why [name] is refused, the spaces around it apart: blank, too long, or taken ([isTaken]); null when it is not. */
    fun problemOf(
        name: String,
        apartFrom: Id? = null,
    ): NameProblem? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> NameProblem.BLANK
            !fitsInAName(trimmed) -> NameProblem.TOO_LONG
            isTaken(trimmed, apartFrom) -> NameProblem.TAKEN
            else -> null
        }
    }

    /**
     * A name that nothing of the list other than [apartFrom] has, the one offered to take (« Prendre « Joyeux 2 » »):
     * [wanted] as it is, without the spaces around it, when it is free; else [wanted] followed by the first number
     * from 2 that makes it free. A [wanted] name that already ends with a number counts on from it: « Joyeux 3 » after
     * « Joyeux 2 »; a number too big to count on is part of the name.
     */
    fun freeName(
        wanted: String,
        apartFrom: Id? = null,
    ): String {
        val name = wanted.trim()
        if (!isTaken(name, apartFrom)) return name
        val number = ENDING_NUMBER.find(name)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it < Int.MAX_VALUE }
        return if (number == null) {
            numbered(name, from = 2, apartFrom)
        } else {
            numbered(name.substringBeforeLast(' '), from = number + 1, apartFrom)
        }
    }

    /** « [base] n », with the first number from [from] that nothing of the list other than [apartFrom] has. */
    private fun numbered(
        base: String,
        from: Int,
        apartFrom: Id?,
    ): String = generateSequence(from) { it + 1 }.map { "$base $it" }.first { !isTaken(it, apartFrom) }

    /**
     * One of [candidates], drawn with [random], of [MAX_LENGTH] characters at the most: one that nothing of the list
     * has, else one followed by the first number that makes it free ([freeName]), else, when none of them fits once
     * numbered, one cut so that its number fits ([cutFreeName]).
     */
    fun proposedAmong(
        candidates: List<String>,
        random: Random,
    ): String {
        val free = candidates.filterNot { isTaken(it) }.filter(::fitsInAName)
        val numberedFree = free.ifEmpty { candidates.map { freeName(it) }.filter(::fitsInAName) }
        return numberedFree.ifEmpty { candidates.map(::cutFreeName) }.random(random)
    }

    /**
     * [wanted] cut at [MAX_LENGTH] characters, without the spaces around it, when nothing of the list has it; else cut
     * shorter, so that the first number from 2 that makes it free still fits.
     */
    private fun cutFreeName(wanted: String): String {
        val whole = cutName(wanted).trim()
        if (!isTaken(whole)) return whole
        return generateSequence(2) { it + 1 }
            .map { number -> " $number".let { cutTo(whole, MAX_LENGTH - it.length).trimEnd() + it } }
            .first { !isTaken(it) }
    }

    companion object {
        /** The most characters of a name: a name fits on a card of a list, and in the file that keeps it. */
        const val MAX_LENGTH: Int = 40

        /** A number at the end of a name, after a space: « Joyeux 2 ». */
        private val ENDING_NUMBER = Regex(""" ([0-9]+)$""")
    }
}

/** Why something cannot take a name. */
enum class NameProblem {
    /** Empty, or only spaces. */
    BLANK,

    /** More characters than a name may have, [TakenNames.MAX_LENGTH], the spaces around it apart. */
    TOO_LONG,

    /** Another of the list has it, the case and the spaces around it ignored ([TakenNames.isTaken]). */
    TAKEN,
}

/**
 * The name [typed] (in an editor, or in a list), cut at the most characters a name may have, [TakenNames.MAX_LENGTH],
 * never between the two units of a character; a shorter name as it is, spaces kept.
 */
fun cutName(typed: String): String = cutTo(typed, TakenNames.MAX_LENGTH)

/** [text] cut at [characters] characters, never between the two units of a character. */
private fun cutTo(
    text: String,
    characters: Int,
): String = text.take(text.offsetByCodePoints(0, minOf(text.codePointCount(0, text.length), characters)))

/** Whether [name] has [TakenNames.MAX_LENGTH] characters at the most. */
private fun fitsInAName(name: String): Boolean = name.codePointCount(0, name.length) <= TakenNames.MAX_LENGTH
