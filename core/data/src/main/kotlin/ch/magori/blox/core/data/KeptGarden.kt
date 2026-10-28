package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import java.time.Instant

/**
 * A garden of the player, kept in « Mes jardins » (P10.27): what the Jardin musical needs to open it again as it was
 * kept. [id] is its identity, which a rename or a replacement leaves unchanged; [name] the one the player gave it, of
 * [MAX_NAME_LENGTH] characters at the most and without spaces around it; [garden] its pieces and its mood; [modifiedAt]
 * the time it was last kept, replaced or renamed, which the library sets.
 */
data class KeptGarden(val id: GardenId, val name: String, val garden: Garden, val modifiedAt: Instant) {
    init {
        require(name.isNotBlank()) { "A kept garden has a name that is not blank" }
        require(name == name.trim()) { "A kept garden has no spaces around its name, was \"$name\"" }
        require(name.codePointCount(0, name.length) <= MAX_NAME_LENGTH) {
            "A kept garden has a name of $MAX_NAME_LENGTH characters at the most, was \"$name\""
        }
    }

    companion object {
        /** The most characters of a name, the one of any list ([TakenNames.MAX_LENGTH]). */
        const val MAX_NAME_LENGTH: Int = TakenNames.MAX_LENGTH
    }
}

/**
 * The identity of a kept garden, as a saved music has one ([ch.magori.blox.core.music.ScoreId.Personal]): a [key] given
 * by the library when the garden is first kept, never by the player.
 */
data class GardenId(val key: String) {
    init {
        require(key.isNotBlank()) { "A kept garden is known by a key that is not blank" }
    }
}
