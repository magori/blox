package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** [content] on a page of the Colorful theme, [alignment] in it, as it shows over a screen. */
@Composable
private fun OnPage(
    alignment: Alignment.Horizontal,
    content: @Composable () -> Unit,
) {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Column(
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(BloxSpaces.space4),
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space4),
        ) {
            content()
        }
    }
}

/**
 * The menu « … » of a card of « Mes musiques », and the checked items of that of the editor, under the « … » that
 * opens it: without the window of the system, which the previews do not draw.
 */
@Preview(widthDp = 360, heightDp = 560)
@Composable
internal fun MenuPreview() {
    OnPage(Alignment.End) {
        GlyphButton(description = "Plus d'actions", onClick = {}, emphasis = ButtonEmphasis.BARE) { ink ->
            Glyphs.more(this, ink)
        }
        MenuPanel {
            BloxMenuItem(text = "Modifier", icon = BloxIcon.EDIT, onClick = {})
            BloxMenuItem(text = "Renommer", icon = BloxIcon.RENAME, onClick = {})
            BloxMenuItem(text = "Ne plus la jouer pendant les parties", icon = BloxIcon.NOTE, onClick = {})
            BloxMenuItem(text = "Suivre la lecture", checked = true, onCheckedChange = {})
            BloxMenuItem(text = "Voir l'autre piste", checked = false, onCheckedChange = {})
            BloxMenuItem(text = "Supprimer", icon = BloxIcon.TRASH, onClick = {}, danger = true)
        }
    }
}

/** A message with its action and its cross, and a message alone. */
@Preview(widthDp = 360, heightDp = 360)
@Composable
internal fun SnackMessagePreview() {
    OnPage(Alignment.CenterHorizontally) {
        SnackMessage(
            text = "« Korobeïniki variée » est supprimée.",
            action = MessageAction(label = "Annuler", onClick = {}),
            onClose = {},
        )
        SnackMessage(text = "Mesure copiée.")
    }
}
