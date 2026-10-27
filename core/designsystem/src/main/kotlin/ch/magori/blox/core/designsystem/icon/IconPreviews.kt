package ch.magori.blox.core.designsystem.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** The sizes of the sheet: above a label, beside a label, on a key of the game, and large, to see the drawing. */
private val SheetSizes = listOf(IconSizes.aboveLabel, IconSizes.besideLabel, 24.dp, 48.dp)

/** Every icon of the theme [kind], one per line, at each of [SheetSizes], in the ink of the page. */
@Composable
private fun IconSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        val palette = BloxTheme.palette
        Column(
            modifier = Modifier.fillMaxSize().background(palette.page).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BloxIcon.entries.forEach { icon ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = icon.name,
                        color = palette.ink,
                        fontSize = 9.sp,
                        style = BloxTheme.typography.label,
                        modifier = Modifier.width(120.dp),
                    )
                    SheetSizes.forEach { size -> BloxIcon(icon, size = size, tint = palette.ink) }
                }
            }
        }
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 1700)
@Composable
internal fun IconSheetOriginalPreview() {
    IconSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 1700)
@Composable
internal fun IconSheetColorfulPreview() {
    IconSheet(BloxThemeKind.COLORFUL)
}
