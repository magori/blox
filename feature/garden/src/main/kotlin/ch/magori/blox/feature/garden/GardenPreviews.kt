package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenComposer
import ch.magori.blox.core.garden.GardenMood

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/**
 * [state] under the Original theme of the application, in [effects]: the garden draws itself in Colorful, its effects
 * frozen at [tick], over by default.
 */
@Composable
private fun GardenPreview(
    state: GardenUiState?,
    tick: Int = Int.MAX_VALUE,
    effects: BloxEffects = BloxEffects.Full,
) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL, effects = effects) {
        GardenScreen(uiState = state, actions = GardenActions.None, effectTick = tick)
    }
}

/** The garden as it opens the first time, empty and calm. */
private val EMPTY = GardenUiState(Garden.EMPTY)

/**
 * Four pieces placed in the joyful mood, as V40 « refus » draws them, and the Z shown over the T, refused: a cell
 * crossed out, the message telling why.
 */
private val REFUSED = GardenUiState(
    garden = Garden.parse(
        """
        ................
        ................
        ................
        ................
        ................
        ................
        ................
        ...IIII.........
        ................
        LLL.............
        L....TTT........
        ..J...T.........
        ..J.............
        .JJ.............
        """,
    ).withMood(GardenMood.JOYFUL),
    chosen = PieceType.Z,
    aim = Position(7, 9),
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun GardenEmptyPreview() = GardenPreview(EMPTY)

@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
internal fun GardenEmptyNightPreview() = GardenPreview(EMPTY)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenRefusedPreview() = GardenPreview(REFUSED)

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun GardenRefusedNightPreview() = GardenPreview(REFUSED)

/** In a font twice as large on a narrow phone: the page scrolls, Poser stays at hand. */
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
internal fun GardenLargeFontPreview() = GardenPreview(REFUSED)

/** « Jardin effacé · Annuler » after Tout effacer. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenClearedPreview() = GardenPreview(GardenUiState(Garden.EMPTY, undo = REFUSED.garden))

/** The garden of [REFUSED] listened to, without a preview: column 6 played, the pill of its chord filled (V40). */
private val LISTENING = GardenUiState(garden = REFUSED.garden, playedColumn = 5)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenListeningPreview() = GardenPreview(LISTENING)

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun GardenListeningNightPreview() = GardenPreview(LISTENING)

/**
 * The T of [REFUSED] just placed, lit on the second tick of its feedback, while the garden is listened to: the notes of
 * column 3 bouncing on the second tick of its eighth.
 */
private val PLACED = REFUSED.garden.let { garden ->
    GardenUiState(
        garden = garden,
        playedColumn = 2,
        change = GardenChange.Placed(garden.pieceAt(Position(6, 10))!!),
        move = 1,
    )
}

/** The tick the feedbacks are frozen at in [PLACED]: the bounce at its largest. */
private const val PLACED_TICK = 2

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenPlacedPreview() = GardenPreview(PLACED, tick = PLACED_TICK)

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun GardenPlacedNightPreview() = GardenPreview(PLACED, tick = PLACED_TICK)

/** A garden Au hasard composes in the joyful mood, just rolled. */
private val ROLLED = GardenUiState(
    garden = GardenComposer.compose(GardenMood.JOYFUL, seed = ROLLED_SEED),
    change = GardenChange.Rolled,
    move = 1,
)

/** The seed of the garden of [ROLLED]. */
private const val ROLLED_SEED = 7L

/** On its seventh tick, Au hasard has dropped its first four pieces, the fourth one still falling. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenRolledPreview() = GardenPreview(ROLLED, tick = 6)

/** In reduced effects, on its fifth tick, the pieces of Au hasard have half faded in, together, where they lie. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenRolledReducedPreview() =
    GardenPreview(ROLLED, tick = 4, effects = BloxEffects(reduced = true, forcedBySystem = false))
