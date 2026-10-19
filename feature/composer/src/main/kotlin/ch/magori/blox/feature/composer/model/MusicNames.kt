package ch.magori.blox.feature.composer.model

/**
 * The names of the saved musics, [names], as the save dialog weighs them, the music being edited, called [current],
 * left out: `null` for a music never saved. Two names are the same whatever their case and the spaces around them.
 */
internal class MusicNames(names: Collection<String>, current: String? = null) {
    private val others = names.filterNot { current != null && it.sameAs(current) }

    /** The name proposed for a music of the category called [base]: `base n`, with the first number not taken. */
    fun proposed(base: String): String = numbered(base, from = 1)

    /** Whether another music is called [name]. */
    fun isTaken(name: String): Boolean = others.any { it.sameAs(name) }

    /**
     * The free name offered for a taken [name]: `name n`, with the first number from 2 not taken; a name that already
     * ends with a number goes on from the next one (« Thème 2 » offers « Thème 3 »).
     */
    fun free(name: String): String {
        val trimmed = name.trim()
        val number = ENDING_NUMBER.find(trimmed)
        return if (number == null) {
            numbered(trimmed, from = 2)
        } else {
            numbered(trimmed.substring(0, number.range.first), from = number.groupValues[1].toInt() + 1)
        }
    }

    private fun numbered(
        base: String,
        from: Int,
    ): String = generateSequence(from) { it + 1 }.map { "$base $it" }.first { !isTaken(it) }

    private fun String.sameAs(other: String): Boolean = trim().equals(other.trim(), ignoreCase = true)

    private companion object {
        /** A number at the end of a name, after a space: the space is left out of the base. */
        val ENDING_NUMBER = Regex(" ([0-9]+)$")
    }
}
