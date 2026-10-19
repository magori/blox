package ch.magori.blox.core.data

import ch.magori.blox.core.music.ScoreId

/*
 * The rules of the names of the saved musics, the only source of them: the library keeps them (it refuses a name
 * taken), the music editor shows them (the name it proposes, the free one it offers). A name is always weighed
 * without the spaces around it, and whatever its case; the music being saved or renamed, known by its identity
 * [ScoreId.Personal], never takes its own name.
 */

/** Whether one of these musics other than [apartFrom] is called [name], the case and the spaces around it ignored. */
fun List<SavedMusic>.isNameTaken(
    name: String,
    apartFrom: ScoreId.Personal? = null,
): Boolean = any { it.id != apartFrom && it.name.equals(name.trim(), ignoreCase = true) }

/**
 * The name proposed for a music of the category called [base] (« Joyeux »): « Joyeux n », the first number from 1
 * that no music other than [apartFrom] has.
 */
fun List<SavedMusic>.proposedName(
    base: String,
    apartFrom: ScoreId.Personal? = null,
): String = numbered(base.trim(), from = 1, apartFrom)

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

/** A number at the end of a name, after a space: « Joyeux 2 ». */
private val ENDING_NUMBER = Regex(""" ([0-9]+)$""")
