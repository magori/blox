package ch.magori.blox.feature.composer

import androidx.lifecycle.SavedStateHandle
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.pitch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

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

/** The seed of the draws of the dice, in each test. */
internal const val DICE_SEED = 42L

/**
 * An editor on the music of [key], a new one when null, in [library], restored from [savedState]; its dice draws with
 * [random] and composes on [generation], by default the main dispatcher of the test, at once; it rolls once open if it
 * is to [rollDice].
 */
internal fun composer(
    music: MusicPlayer,
    library: MusicLibrary,
    key: String? = null,
    savedState: SavedStateHandle = SavedStateHandle(),
    random: Random = Random(DICE_SEED),
    generation: CoroutineDispatcher = Dispatchers.Main,
    rollDice: Boolean = false,
) = ComposerViewModel(music, library, key, savedState, random, generation, rollDice)

/** Musics whose saves wait for [gate] before they are written, counted in [saves]. */
internal class SlowLibrary(private val delegate: MusicLibrary) : MusicLibrary by delegate {
    val gate = CompletableDeferred<Unit>()
    var saves = 0

    override suspend fun save(music: SavedMusic) {
        saves++
        gate.await()
        delegate.save(music)
    }
}
