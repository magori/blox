package ch.magori.blox.core.data

/*
 * The rules of the names of the kept gardens: the library keeps them (it refuses a name taken), the sheet « Mes
 * jardins » and its naming window show them (why a name is refused; the name proposed). They are the rules of the
 * names of any list, [TakenNames], on the gardens known by their identity [GardenId], a list apart from the one of the
 * saved musics: a name is always weighed without the spaces around it, and whatever its case; the garden being renamed
 * never takes its own.
 */

/** The names of these gardens, each by its identity. */
internal fun List<KeptGarden>.takenNames(): TakenNames<GardenId> = TakenNames(associate { it.id to it.name })

/**
 * Why a garden other than these, or [apartFrom] among them, cannot be called [name]: blank, too long, or taken by
 * another garden, the case and the spaces around it ignored; null when it can.
 */
fun List<KeptGarden>.nameProblem(
    name: String,
    apartFrom: GardenId? = null,
): NameProblem? = takenNames().problemOf(name, apartFrom)

/**
 * A name that no garden other than [apartFrom] has, the one offered to take: [wanted] as it is, without the spaces
 * around it, when it is free; else [wanted] followed by the first number from 2 that makes it free
 * ([TakenNames.freeName]).
 */
fun List<KeptGarden>.freeName(
    wanted: String,
    apartFrom: GardenId? = null,
): String = takenNames().freeName(wanted, apartFrom)

/**
 * The name proposed for a garden heard in the mood called [moodName], as the screen shows it (« Calme »): « Jardin »
 * then that name in lower case, « Jardin calme », numbered if another garden has it ([freeName]): « Jardin calme 2 ».
 * The names of the moods are resources of the screen, which `core:data` does not read: the screen gives the name.
 */
fun List<KeptGarden>.proposedName(moodName: String): String = freeName("$GARDEN_WORD ${moodName.lowercase()}")

/** The word a proposed name starts with. */
private const val GARDEN_WORD = "Jardin"
