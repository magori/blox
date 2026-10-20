package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.isNameTaken
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId

/**
 * The state of the editor: the [draft] being written and its [saved] version, `null` for a music never saved, the
 * [track] whose tab is open, the [bar], counted from 0, its grid shows, the [mode] of the touches, and the draft that
 * *Undo* brings back, [undoable], if any.
 */
internal data class EditorState(
    val draft: ScoreDraft,
    val saved: ScoreDraft? = null,
    val track: Track = Track.MELODY,
    val bar: Int = 0,
    val mode: EditMode = EditMode.PLACE,
    val undoable: ScoreDraft? = null,
) {
    init {
        requireBar(bar)
    }

    /** Whether *Copy the bar* has an effect: not on the last bar, which has no bar after it. */
    val canCopyBar: Boolean get() = bar < BARS - 1

    /** Whether *Clear the bar* has an effect: not when no note sounds in the bar of the open track. */
    val canClearBar: Boolean get() = !draft[track].isBarEmpty(bar)

    /** Whether *Undo* has something to bring back. */
    val canUndo: Boolean get() = undoable != null

    /** Where the music stands against its saved version. */
    val status: SaveStatus
        get() = when (saved) {
            null -> SaveStatus.NOT_SAVED
            draft -> SaveStatus.SAVED
            else -> SaveStatus.MODIFIED
        }

    /**
     * Whether leaving asks to save first: for a saved music that was changed, and for a new music unless it was left
     * empty, which has nothing to lose.
     */
    val asksBeforeLeaving: Boolean
        get() = when (status) {
            SaveStatus.SAVED -> false
            SaveStatus.MODIFIED -> true
            SaveStatus.NOT_SAVED -> !draft.isEmpty
        }

    /** Touching the [eighth] cell of the bar on the line of [pitch], as the [mode] says. */
    fun touch(
        pitch: Pitch,
        eighth: Int,
    ): EditorState {
        val cell = Cell(bar, eighth)
        return edit { grid ->
            when (mode) {
                EditMode.PLACE -> grid.place(pitch, cell)
                EditMode.LENGTHEN -> grid.lengthen(pitch, cell)
            }
        }
    }

    /** Touching the [fromEighth] cell of the bar on the line of [pitch] and sliding to the [toEighth] cell. */
    fun slide(
        pitch: Pitch,
        fromEighth: Int,
        toEighth: Int,
    ): EditorState = edit { it.slide(pitch, Cell(bar, fromEighth), Cell(bar, toEighth)) }

    /**
     * *Copy the bar*: the bar of the open track copied over the next one, which the grid then shows; *Undo* brings the
     * next bar back. Refused with an `IllegalStateException` when [canCopyBar] is false.
     */
    fun copyBar(): EditorState {
        check(canCopyBar) { "The last bar has no bar after it to copy it over" }
        return edit { it.copyBar(bar) }.copy(bar = bar + 1, undoable = draft)
    }

    /**
     * *Clear the bar*: the bar of the open track left silent; *Undo* brings it back. Refused with an
     * `IllegalStateException` when [canClearBar] is false.
     */
    fun clearBar(): EditorState {
        check(canClearBar) { "No note sounds in this bar of the open track" }
        return edit { it.clearBar(bar) }.copy(undoable = draft)
    }

    /**
     * The dice: [generated] in place of the music, from its first bar. *Undo* brings the music back, unless its grids
     * were empty: there is nothing to bring back.
     */
    fun rollDice(generated: ScoreDraft): EditorState =
        copy(draft = generated, bar = 0, undoable = draft.takeUnless { it.isEmpty })

    /** The music [Tempo.STEP] beats per minute faster; it takes the undo away. */
    fun faster(): EditorState = changed(draft.copy(tempo = draft.tempo.faster()))

    /** The music [Tempo.STEP] beats per minute slower; it takes the undo away. */
    fun slower(): EditorState = changed(draft.copy(tempo = draft.tempo.slower()))

    /** *Undo*: the draft as it was before the last change that can be undone. It can be undone once only. */
    fun undo(): EditorState = changed(checkNotNull(undoable) { "Nothing to undo" })

    /**
     * Why the music cannot be saved under [name], given the saved [musics], the one being edited, known by [apartFrom],
     * left out (null for a music never saved); `null` when it can.
     */
    fun saveProblem(
        name: String,
        musics: List<SavedMusic>,
        apartFrom: ScoreId.Personal? = null,
    ): SaveProblem? = if (draft.isEmpty) SaveProblem.EMPTY_GRID else renameProblem(name, musics, apartFrom)

    /** The editor once the music is saved: its changes are counted from there. */
    fun markedSaved(): EditorState = copy(saved = draft)

    /** The editor with the grid of the open track changed by [change], which takes the undo away. */
    private fun edit(change: (TrackGrid) -> TrackGrid): EditorState = changed(draft.with(track, change(draft[track])))

    /** The editor on [draft], without undo. */
    private fun changed(draft: ScoreDraft): EditorState = copy(draft = draft, undoable = null)

    companion object {
        /** The editor on a new, empty music. */
        fun new(): EditorState = EditorState(ScoreDraft.new())

        /** The editor on a saved music, [draft]. */
        fun open(draft: ScoreDraft): EditorState = EditorState(draft, saved = draft)
    }
}

/**
 * Why a music cannot be called [name], given the saved [musics], the one being renamed, known by [apartFrom], left out
 * (null for a music never saved); `null` when it can.
 */
internal fun renameProblem(
    name: String,
    musics: List<SavedMusic>,
    apartFrom: ScoreId.Personal?,
): SaveProblem? = when {
    name.isBlank() -> SaveProblem.BLANK_NAME
    musics.isNameTaken(name, apartFrom) -> SaveProblem.NAME_TAKEN
    else -> null
}
