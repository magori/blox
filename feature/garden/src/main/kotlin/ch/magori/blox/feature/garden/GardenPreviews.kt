package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** [state] under the Original theme of the application: the garden draws itself in Colorful. */
@Composable
private fun GardenPreview(state: GardenUiState?) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        GardenScreen(uiState = state, actions = GardenActions.None)
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
