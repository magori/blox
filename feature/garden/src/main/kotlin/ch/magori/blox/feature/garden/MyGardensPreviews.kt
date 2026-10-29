package ch.magori.blox.feature.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.designsystem.component.SettledSheet
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenComposer
import ch.magori.blox.core.garden.GardenMood
import java.time.Instant

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** A garden of [mood] that the composer gives from [seed], kept as [name], [minutes] after the epoch. */
private fun keptGarden(
    name: String,
    mood: GardenMood,
    seed: Long,
    minutes: Long,
): KeptGarden = KeptGarden(
    id = GardenId(name),
    name = name,
    garden = GardenComposer.compose(mood, seed),
    modifiedAt = Instant.EPOCH.plusSeconds(minutes * 60),
)

/** Three kept gardens, one of each piece count, the oldest first, as the library gives them. */
private val GARDENS = listOf(
    keptGarden("Pluie douce", GardenMood.CALM, seed = 3, minutes = 1),
    keptGarden("Marche des géants dans la brume du matin", GardenMood.EPIC, seed = 5, minutes = 2),
    keptGarden("Fête", GardenMood.JOYFUL, seed = 7, minutes = 3),
)

/** The garden under way, opened from « Pluie douce » and changed since: its mood. */
private val MODIFIED = GardenUiState(
    garden = GARDENS.first().garden.withMood(GardenMood.MYSTERIOUS),
    keptFrom = GARDENS.first().id,
    myGardens = MyGardens.Listed(GARDENS),
)

/** The garden under way, [state], under the sheet « Mes jardins » as it stands once come in. */
@Composable
private fun MyGardensPreview(state: GardenUiState) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        Box {
            GardenScreen(uiState = state, actions = GardenActions.None)
            ColorfulTheme {
                MyGardensSheetContent(state, GardenActions.None, messageMillis = 0) { title, content ->
                    SettledSheet(title = title, content = content)
                }
            }
        }
    }
}

/** The garden under way changed since it was kept: the dot of « Mes jardins », on a narrow phone. */
@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun GardenModifiedPreview() = BloxTheme(kind = BloxThemeKind.ORIGINAL) {
    GardenScreen(uiState = MODIFIED, actions = GardenActions.None)
}

/** The same in a font twice as large. */
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
internal fun GardenModifiedLargeFontPreview() = GardenModifiedPreview()

/** « « Pluie douce » gardé. », just kept. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenKeptPreview() = BloxTheme(kind = BloxThemeKind.ORIGINAL) {
    GardenScreen(uiState = MODIFIED.copy(garden = GARDENS.first().garden, keptName = "Pluie douce"), GardenActions.None)
}

/** The list, from the most recent, the garden under way coming from the oldest, changed since. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun MyGardensListPreview() = MyGardensPreview(MODIFIED)

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun MyGardensListNightPreview() = MyGardensPreview(MODIFIED)

/** The list in a font twice as large on a narrow phone: the names on 2 lines, the sheet scrolls. */
@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
internal fun MyGardensListLargeFontPreview() = MyGardensPreview(MODIFIED)

/** No garden kept yet, the empty garden under way: nothing to keep, and why. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun MyGardensEmptyPreview() =
    MyGardensPreview(GardenUiState(Garden.EMPTY, myGardens = MyGardens.Listed(emptyList())))

/** 30 gardens kept: « Garder une copie » greyed, and why; « Enregistrer « X » » still possible. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun MyGardensFullPreview() = MyGardensPreview(
    MODIFIED.copy(
        myGardens = MyGardens.Listed(
            GARDENS + List(GardenLibrary.MAX_GARDENS - GARDENS.size) {
                keptGarden("Jardin ${it + 1}", GardenMood.CALM, seed = it.toLong(), minutes = 0)
            },
        ),
    ),
)

/** The gardens of a later version: nothing can be kept, and why. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun MyGardensUnreadablePreview() =
    MyGardensPreview(GardenUiState(GARDENS.first().garden, myGardens = MyGardens.Unreadable))

/** The window of the name, a name taken typed: why under the field, Garder disabled. */
@Composable
private fun NamingPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        ColorfulTheme {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(BloxTheme.palette.page)
                    .padding(horizontal = BloxSizes.windowMargin),
            ) {
                GardenNamingWindow(
                    naming = GardenNaming(name = "Pluie douce", problem = NameProblem.TAKEN),
                    copy = false,
                    actions = GardenActions.None,
                    messageMillis = 0,
                )
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun GardenNamingPreview() = NamingPreview()

@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
internal fun GardenNamingLargeFontPreview() = NamingPreview()
