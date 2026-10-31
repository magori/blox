package ch.magori.blox.feature.composer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

/** The seed the dice and the names of the editors of the tests draw from. */
internal const val EDITOR_SEED = 42L

/** The tag of a cell of the grid. */
internal const val CELL_TAG = "composer_cell"

/** The narrowest phone BloX is made for. */
internal val NARROWEST_PHONE = 360.dp

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

/** Shows the editor of [viewModel] in [kind], its font [fontScale] times as large; [onBack] once it is left. */
internal fun ComposeContext.showComposer(
    viewModel: ComposerViewModel,
    kind: BloxThemeKind = BloxThemeKind.COLORFUL,
    modifier: Modifier = Modifier,
    fontScale: Float = 1f,
    onBack: () -> Unit = {},
) {
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            BloxTheme(kind = kind) {
                ComposerScreen(viewModel = viewModel, onBack = onBack, onDeleted = {}, modifier = modifier)
            }
        }
    }
}
