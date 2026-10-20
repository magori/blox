package ch.magori.blox.feature.composer

import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.music.Category
import ch.magori.blox.feature.composer.model.SaveProblem

/**
 * A window of the editor, over it until it is answered or closed. A window that writes the musics says when it failed
 * to (Save.failed, Rename.failed, Delete.failed), and stays open.
 */
internal sealed interface ComposerDialog {
    /**
     * A window where a [name] is typed: why the music cannot take it, [problem], if anything stops it, and the free
     * name offered when it is taken, [freeName].
     */
    sealed interface Naming : ComposerDialog {
        val name: String
        val problem: SaveProblem?
        val freeName: String?
        val failed: Boolean
    }

    /**
     * *Enregistrer* (V9, V12): the [name] typed, whether the music is to be played during the games, [forGames], and its
     * [style], a category or Libre (null). Another style proposes another name until the name is chosen, [nameChosen]:
     * typed, taken free, or the one of a music named already. Once saved, the editor is left when the window came from
     * leaving it, [thenLeave].
     */
    data class Save(
        override val name: String,
        val forGames: Boolean,
        val thenLeave: Boolean = false,
        val style: Category? = null,
        val nameChosen: Boolean = false,
        override val problem: SaveProblem? = null,
        override val freeName: String? = null,
        override val failed: Boolean = false,
    ) : Naming

    /** *Renommer*: the [name] typed. */
    data class Rename(
        override val name: String,
        override val problem: SaveProblem? = null,
        override val freeName: String? = null,
        override val failed: Boolean = false,
    ) : Naming

    /** Leaving a music with changes not saved: *Enregistrer*, *Quitter sans enregistrer* or *Continuer*. */
    data object Leave : ComposerDialog

    /**
     * *Supprimer*, confirmed (*Supprimer*) or not (*Garder la musique*); a music played during the games, [isGameMusic],
     * warns that the music of the home screen takes its place.
     */
    data class Delete(val isGameMusic: Boolean, val failed: Boolean = false) : ComposerDialog
}

/** How the screen leaves the editor, once the editor says so. */
internal sealed interface ComposerExit {
    /** Back to the screen before. */
    data object Leave : ComposerExit

    /**
     * Back to the screen before, the [music] deleted. Nothing restores it yet: « Annuler » after a deletion from the
     * editor comes with P4.19e.
     */
    data class Deleted(val music: DeletedMusic) : ComposerExit
}
