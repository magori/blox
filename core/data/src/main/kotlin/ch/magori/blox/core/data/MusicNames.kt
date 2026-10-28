package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScoreId
import kotlin.random.Random

/*
 * The rules of the names of the saved musics: the library keeps them (it refuses a name taken), the list and the music
 * editor show them (why a name is refused; the name the editor proposes, the free one it offers). They are the rules of
 * the names of any list, [TakenNames], on the musics known by their identity [ScoreId.Personal]: a name is always
 * weighed without the spaces around it, and whatever its case; the music being saved or renamed never takes its own.
 */

/** Whether one of these musics other than [apartFrom] is called [name], the case and the spaces around it ignored. */
fun List<SavedMusic>.isNameTaken(
    name: String,
    apartFrom: ScoreId.Personal? = null,
): Boolean = takenNames().isTaken(name, apartFrom)

/** The names of these musics, each by its identity. */
private fun List<SavedMusic>.takenNames(): TakenNames<ScoreId.Personal> = TakenNames(associate { it.id to it.name })

/**
 * Why a music other than these, or [apartFrom] among them, cannot be called [name]: blank, too long, or taken
 * ([isNameTaken]), the spaces around it apart; null when it can. The rule of the list and of the editor.
 */
fun List<SavedMusic>.nameProblem(
    name: String,
    apartFrom: ScoreId.Personal? = null,
): NameProblem? = takenNames().problemOf(name, apartFrom)

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

/** One of [names], drawn with [random], by the rule of every list ([TakenNames.proposedAmong]). */
internal fun List<SavedMusic>.proposedNameAmong(
    names: List<String>,
    random: Random,
): String = takenNames().proposedAmong(names, random)

/**
 * A name that no music other than [apartFrom] has, the one the editor offers to take (« Prendre « Joyeux 2 » »):
 * [wanted] as it is, without the spaces around it, when it is free; else [wanted] followed by the first number from 2
 * that makes it free. A [wanted] name that already ends with a number counts on from it: « Joyeux 3 » after
 * « Joyeux 2 »; a number too big to count on is part of the name.
 */
fun List<SavedMusic>.freeName(
    wanted: String,
    apartFrom: ScoreId.Personal? = null,
): String = takenNames().freeName(wanted, apartFrom)
