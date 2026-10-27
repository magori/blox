package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** The categories of the dice of the editor, each with what it sounds like: Marche is chosen. */
private val Categories = listOf(
    "Surprise" to "Une catégorie au hasard",
    "Marche" to "Pas réguliers, entraînants",
    "Berceuse" to "Lente et douce",
    "Valse" to "Trois temps qui tournent",
    "Fanfare" to "Éclatante, en accords",
)

/**
 * The sheet Au hasard of the editor, come in, over a page of Couleurs (day or night, as the system says), without the
 * window of the system: its choices, Marche chosen, then Tirer une musique.
 */
@Composable
private fun DiceSheetPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Box(Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(BloxSpaces.space4)) {
            WindowText("Thème du soir")
        }
        SheetFrame(
            title = "Au hasard",
            onDismissRequest = {},
            insets = WindowInsets(0),
            entrance = { SheetEntrance.Settled },
            actions = {
                PrimaryButton(
                    text = "Tirer une musique",
                    icon = BloxIcon.DICE,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            SheetChoices {
                Categories.forEach { (label, subLine) ->
                    SheetChoiceRow(label = label, subLine = subLine, selected = label == "Marche", onClick = {})
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun SheetColorfulPreview() = DiceSheetPreview()

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun SheetColorfulLargeFontPreview() = DiceSheetPreview()
