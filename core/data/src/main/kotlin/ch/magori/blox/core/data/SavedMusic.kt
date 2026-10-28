package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import java.time.Instant

/**
 * A music of the player, kept in « Mes musiques » (P4.22): what the music editor needs to open it again as it was
 * saved. [id] is its identity, which a rename leaves unchanged, and the one of its [score]; [name] is the one the
 * player gave it, of [MAX_NAME_LENGTH] characters at the most and without spaces around it; [category] the one it was
 * generated in, null for a music written by hand; [scale] the scale its grid follows, tonic included; [score] its two
 * voices, its tempo ([Score.bpm]) and its bars; [modifiedAt] the time it was last saved or renamed, which the library
 * sets, null when it is unknown: a music saved before the musics file kept it (version 1).
 */
data class SavedMusic(
    val id: ScoreId.Personal,
    val name: String,
    val category: Category?,
    val scale: Scale,
    val score: Score,
    val modifiedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "A saved music has a name that is not blank" }
        require(name == name.trim()) { "A saved music has no spaces around its name, was \"$name\"" }
        require(name.codePointCount(0, name.length) <= MAX_NAME_LENGTH) {
            "A saved music has a name of $MAX_NAME_LENGTH characters at the most, was \"$name\""
        }
        require(score.id == id) { "A saved music is known by the identity of its score: $id, was ${score.id}" }
    }

    companion object {
        /** The most characters of a name, the one of any list ([TakenNames.MAX_LENGTH]). */
        const val MAX_NAME_LENGTH: Int = TakenNames.MAX_LENGTH
    }
}
