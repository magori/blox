package ch.magori.blox.feature.musics

import ch.magori.blox.core.audio.RingtoneKind
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId

/**
 * What « Mes musiques » shows: the saved [musics], in their order, the name of the one played during the games,
 * [gameMusic], null when the games play the music chosen on the home screen, the [dialog] open, if any, and the name of
 * the music [deleted] last, while « Annuler » may put it back; whether the menu of a music offers « Sonnerie ou
 * réveil », [ringtoneOffered], only when the device may write one (Android 10 and later, ADR 0031), and the sheet
 * « Sonnerie ou réveil » open, [ringtone], if any.
 */
internal data class MyMusicsUiState(
    val musics: List<MusicCard>,
    val gameMusic: String?,
    val dialog: MyMusicsDialog? = null,
    val deleted: String? = null,
    val ringtoneOffered: Boolean = false,
    val ringtone: RingtoneSheetState? = null,
)

/**
 * The sheet « Sonnerie ou réveil » (V42) of the music of [id], called [name]: the length of the file, in whole
 * [seconds], the use of the file chosen, [kind], where its writing is, [phase], whether it is [listening] to, and
 * whether the volume of the music is at 0 %, [musicMuted], which the sheet says, or *Écouter* would seem broken.
 */
internal data class RingtoneSheetState(
    val id: ScoreId.Personal,
    val name: String,
    val seconds: Int,
    val kind: RingtoneKind,
    val phase: RingtonePhase,
    val listening: Boolean = false,
    val musicMuted: Boolean = false,
)

/** Where the writing of a ringtone is. */
internal sealed interface RingtonePhase {
    /** The use is being chosen: nothing is written yet. */
    data object Choosing : RingtonePhase

    /** The file is being written: the sheet takes no choice until it is done. */
    data object Saving : RingtonePhase

    /**
     * The file is written, as a [kind]: a ringtone, in the ringtones of the phone, chosen in the sound settings; an
     * alarm, chosen in the Clock.
     */
    data class Saved(val kind: RingtoneKind) : RingtonePhase

    /** The file could not be written: the sheet says it, keeps its choices, and offers *Réessayer*. */
    data object Failed : RingtonePhase
}

/**
 * The card of a saved music: its [name], its [category] (null for a music written by hand, « Libre »), its [key] in
 * French (« sol majeur »), its [length] at its tempo (« 0:16 »), as TalkBack says it, [spokenLength] (« 16 secondes »),
 * whether it is played during the games, [inGame], and where it is, [listening] to it, null when it is not.
 */
internal data class MusicCard(
    val id: ScoreId.Personal,
    val name: String,
    val category: Category?,
    val key: String,
    val length: String,
    val spokenLength: String,
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
