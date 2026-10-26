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
                choices = listOf(Choice("Classique"), Choice("Moderne", available = false)),
                selected = 0,
                onChosen = {},
            )
            ChoiceGroup(
                label = "Thème",
                choices = listOf(Choice("Original"), Choice("Couleurs")),
                selected = 1,
                onChosen = {},
            )
            ChoiceGroup(
                label = "Niveau de départ",
                choices = (0..9).map { Choice(it.toString()) },
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
