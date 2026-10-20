package ch.magori.blox.feature.composer

import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.pitch

/** The identity of the first music a new library gives: the one of a new music, in each test. */
internal val NEW_ID = ScoreId.Personal("music-1")

/** What the editor shows, once it has opened its music. */
internal val ComposerViewModel.shown: ComposerUiState get() = checkNotNull(uiState.value) { "Not opened yet" }

/** Saves a music called [name], known by [key], a note on its first cell, and gives its draft. */
internal suspend fun MusicLibrary.saveMusic(
    name: String,
    key: String,
    forGames: Boolean = false,
): ScoreDraft {
    val id = ScoreId.Personal(key)
    val draft = EditorState.new().touch(pitch("C5"), eighth = 0).draft
    save(SavedMusic(id, name, draft.category, draft.scale, draft.toScore(id)))
    if (forGames) setGameMusic(id)
    return draft
}
