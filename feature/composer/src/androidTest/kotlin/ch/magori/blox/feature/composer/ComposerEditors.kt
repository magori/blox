package ch.magori.blox.feature.composer

import androidx.lifecycle.SavedStateHandle
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.data.MusicLibrary
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

/** The seed the dice and the names of the editors of the tests draw from. */
internal const val EDITOR_SEED = 42L

/**
 * An editor of the tests, playing on [music], on the music of [musicKey] in [library], a new music when null; its dice
 * draws its seeds from [EDITOR_SEED] and composes on the main thread, at once.
 */
internal fun composerViewModel(
    music: MusicPlayer,
    library: MusicLibrary,
    musicKey: String? = null,
) = ComposerViewModel(
    music,
    library,
    musicKey,
    SavedStateHandle(),
    Random(EDITOR_SEED),
    Random(EDITOR_SEED),
    Dispatchers.Main.immediate,
    rollDice = false,
)
