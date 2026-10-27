package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/**
 * The groups of choices of the theme [kind]: Mode, its second choice not yet available, Thème, on the line of their
 * label, and ten levels on two rows, their label above.
 */
@Composable
private fun ChoiceSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        Column(
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChoiceGroup(
                label = "Mode",
                choices = listOf(ChoiceItem("Classique"), ChoiceItem("Moderne", available = false)),
                selected = 0,
                onChosen = {},
            )
            ChoiceGroup(
                label = "Thème",
                choices = listOf(ChoiceItem("Original"), ChoiceItem("Couleurs")),
                selected = 1,
                onChosen = {},
            )
            ChoiceGroup(
                label = "Niveau de départ",
                choices = (0..9).map { ChoiceItem(it.toString()) },
                selected = 5,
                onChosen = {},
                columns = 5,
            )
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 360)
@Composable
internal fun ChoiceGroupsOriginalPreview() {
    ChoiceSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 360)
@Composable
internal fun ChoiceGroupsColorfulPreview() {
    ChoiceSheet(BloxThemeKind.COLORFUL)
}

/**
 * The segments of the music editor in the theme [kind], without a label: the tracks, the melody then the bass
 * retained, each filled in its own colour, and the tools, Poser retained, its check in the place of its icon.
 */
@Composable
private fun ChoiceSegmentsSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        val palette = BloxTheme.palette
        val tracks = listOf(
            ChoiceItem("Mélodie", selectedColors = ChoiceColors(palette.melody.tab, palette.melody.tabInk)),
            ChoiceItem("Basse", selectedColors = ChoiceColors(palette.bass.tab, palette.bass.tabInk)),
        )
        val tools = listOf(ChoiceItem("Poser", icon = BloxIcon.PLACE), ChoiceItem("Allonger", icon = BloxIcon.EXTEND))
        Column(
            modifier = Modifier.fillMaxSize().background(palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChoiceSegments(name = "Piste", choices = tracks, selected = 0, onChosen = {})
            ChoiceSegments(name = "Piste", choices = tracks, selected = 1, onChosen = {})
            ChoiceSegments(name = "Outil", choices = tools, selected = 0, onChosen = {})
        }
    }
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
internal fun ChoiceSegmentsOriginalPreview() {
    ChoiceSegmentsSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
internal fun ChoiceSegmentsColorfulPreview() {
    ChoiceSegmentsSheet(BloxThemeKind.COLORFUL)
}
