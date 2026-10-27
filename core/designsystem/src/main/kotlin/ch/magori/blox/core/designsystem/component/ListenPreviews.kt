package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/**
 * Écouter in the theme [kind] (P5.94): 48 dp at rest, playing (active) and disabled, then 56 dp as in the editor,
 * at rest and playing beside the equalizer, which stands at its first pose: rising stairs.
 */
@Composable
private fun ListenSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        val palette = BloxTheme.palette
        Column(
            modifier = Modifier.fillMaxSize().background(palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "ÉCOUTER", color = palette.label, fontSize = 11.sp, style = BloxTheme.typography.label)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ListenButton(playing = false, musicName = "Menuet", onClick = {})
                ListenButton(playing = true, musicName = "Menuet", onClick = {})
                ListenButton(playing = false, musicName = "Menuet", onClick = {}, enabled = false)
            }
            Text(text = "ÉDITEUR", color = palette.label, fontSize = 11.sp, style = BloxTheme.typography.label)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ListenButton(playing = false, musicName = null, onClick = {}, size = 56.dp)
                ListenButton(playing = true, musicName = null, onClick = {}, size = 56.dp)
                Equalizer()
            }
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 200)
@Composable
internal fun ListenOriginalPreview() {
    ListenSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 200)
@Composable
internal fun ListenColorfulPreview() {
    ListenSheet(BloxThemeKind.COLORFUL)
}
