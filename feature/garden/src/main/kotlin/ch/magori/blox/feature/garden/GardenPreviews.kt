package ch.magori.blox.feature.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.garden.Garden

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** The garden as it opens, empty and calm, under the Original theme of the application: it draws itself in Colorful. */
@Composable
private fun GardenPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        GardenScreen(uiState = GardenUiState(Garden.EMPTY), actions = GardenActions.None)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun GardenEmptyPreview() = GardenPreview()

@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
internal fun GardenEmptyNightPreview() = GardenPreview()
