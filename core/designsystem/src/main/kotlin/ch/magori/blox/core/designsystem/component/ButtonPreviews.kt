package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** The states a sheet shows, each by the label of its button. */
private val SheetStates = listOf(
    "Repos" to ButtonState(),
    "Pressé" to ButtonState(depth = 1f),
    "Désactivé" to ButtonState(enabled = false),
    "Focus" to ButtonState(focused = true),
)

/** The [families] of buttons of the theme [kind], each in each state: at rest, pressed, disabled, focused. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonSheet(
    kind: BloxThemeKind,
    families: List<ButtonFamily>,
) {
    BloxTheme(kind = kind) {
        val palette = BloxTheme.palette
        Column(
            modifier = Modifier.fillMaxSize().background(palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            families.forEach { family ->
                Text(text = family.name, color = palette.label, fontSize = 11.sp, style = BloxTheme.typography.label)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SheetStates.forEach { (name, state) ->
                        BubbleButton(family, state, onClick = {}, interactionSource = null) {
                            if (family == ButtonFamily.ICON) {
                                BloxIcon(BloxIcon.GEAR, size = IconSizes.alone)
                            } else {
                                BloxIcon(BloxIcon.PLAY)
                                Text(text = name)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The families in `accent`, and those in `button`. */
private val AccentFamilies = listOf(ButtonFamily.PRIMARY_XL, ButtonFamily.PRIMARY)
private val PlainFamilies = listOf(ButtonFamily.SECONDARY, ButtonFamily.ICON)

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 600)
@Composable
internal fun AccentButtonsOriginalPreview() {
    ButtonSheet(BloxThemeKind.ORIGINAL, AccentFamilies)
}

@Preview(widthDp = 360, heightDp = 600)
@Composable
internal fun AccentButtonsColorfulPreview() {
    ButtonSheet(BloxThemeKind.COLORFUL, AccentFamilies)
}

@Preview(widthDp = 360, heightDp = 360)
@Composable
internal fun PlainButtonsOriginalPreview() {
    ButtonSheet(BloxThemeKind.ORIGINAL, PlainFamilies)
}

@Preview(widthDp = 360, heightDp = 360)
@Composable
internal fun PlainButtonsColorfulPreview() {
    ButtonSheet(BloxThemeKind.COLORFUL, PlainFamilies)
}
