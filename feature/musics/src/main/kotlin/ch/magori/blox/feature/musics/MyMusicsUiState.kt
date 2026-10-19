package ch.magori.blox.feature.musics

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId

/**
 * What « Mes musiques » shows: the saved [musics], in their order, the name of the one played during the games,
 * [gameMusic], null when the games play the music chosen on the home screen, the [dialog] open, if any, and the name of
 * the music [deleted] last, while « Annuler » may put it back.
 */
internal data class MyMusicsUiState(
    val musics: List<MusicCard>,
    val gameMusic: String?,
    val dialog: MyMusicsDialog? = null,
    val deleted: String? = null,
)

/**
 * The card of a saved music: its [name], its [category] (null for a music written by hand), its tempo [bpm], whether
 * it is played during the games, [inGame], and where it is, [listening] to it, null when it is not.
 */
internal data class MusicCard(
    val id: ScoreId.Personal,
    val name: String,
    val category: Category?,
    val bpm: Int,
    val inGame: Boolean,
    val listening: Listening? = null,
)

/** A music being listened to plays its [bar], counted from 1, of its [bars]: « Lecture · mesure 3 sur 8 ». */
internal data class Listening(val bar: Int, val bars: Int)

/**
 * A dialog of the list, about the music of [id], called [name]. It closes once what it confirms is written; when the
 * write [failed] (a full disk), it stays open and tells it.
 */
internal sealed interface MyMusicsDialog {
    val id: ScoreId.Personal
    val name: String
    val failed: Boolean

    /** *Renommer*: the field opens on its [name]. */
    data class Rename(
        override val id: ScoreId.Personal,
        override val name: String,
        override val failed: Boolean = false,
    ) : MyMusicsDialog

    /**
     * *Supprimer*, to confirm: a music played during the games, [inGame], is told to give way to the music of the home
     * screen.
     */
    data class ConfirmDelete(
        override val id: ScoreId.Personal,
        override val name: String,
        val inGame: Boolean,
        override val failed: Boolean = false,
    ) : MyMusicsDialog
}

/** Why a name is refused. */
internal enum class NameProblem {
    /** Empty, or only spaces. */
    BLANK,

    /** Another music has it, the case and the spaces around it ignored. */
    TAKEN,

    /** More characters than a saved music may have in its name (`SavedMusic.MAX_NAME_LENGTH`). */
    TOO_LONG,
}
