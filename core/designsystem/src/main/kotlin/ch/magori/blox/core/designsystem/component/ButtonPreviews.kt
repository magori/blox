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
import androidx.compose.ui.graphics.Shape
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
                        BubbleButton(
                            family = family,
                            state = state,
                            depth = { state.depth },
                            onClick = {},
                            interactionSource = null,
                        ) {
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

/** A sheet's line of the variants of the music: its name, then [buttons]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VariantLine(
    name: String,
    buttons: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = name, color = BloxTheme.palette.label, fontSize = 11.sp, style = BloxTheme.typography.label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { buttons() }
    }
}

/** A button of [family] in [state], [compact] or not, in [shape] if given, showing [icon] and [label] if given. */
@Composable
private fun SheetButton(
    family: ButtonFamily,
    state: ButtonState,
    icon: BloxIcon,
    label: String? = null,
    compact: Boolean = false,
    shape: Shape = BloxTheme.shapes.pill,
) {
    BubbleButton(
        family = family,
        state = state,
        depth = { state.depth },
        onClick = {},
        interactionSource = null,
        shape = shape,
        compact = compact,
    ) {
        BloxIcon(icon, size = if (label == null) IconSizes.alone else IconSizes.besideLabel)
        if (label != null) Text(text = label)
    }
}

/**
 * The variants of the music (V32 to V34) in the theme [kind]: selected, at rest and pressed; active; bare, at rest and
 * pressed; and compact, in a pill and in a tile.
 */
@Composable
private fun ButtonVariantsSheet(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        val selected = ButtonState(selected = true)
        val bare = ButtonState(bare = true)
        Column(
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VariantLine("RETENU") {
                SheetButton(ButtonFamily.SECONDARY, selected, BloxIcon.CHECK, "Repos", compact = true)
                SheetButton(ButtonFamily.SECONDARY, selected.copy(depth = 1f), BloxIcon.CHECK, "Pressé", compact = true)
                SheetButton(ButtonFamily.ICON, selected, BloxIcon.CHECK)
                SheetButton(ButtonFamily.ICON, selected.copy(depth = 1f), BloxIcon.CHECK)
            }
            VariantLine("EN COURS") {
                SheetButton(ButtonFamily.ICON, ButtonState(active = true), BloxIcon.STOP)
                SheetButton(ButtonFamily.ICON, ButtonState(active = true, depth = 1f), BloxIcon.STOP)
            }
            VariantLine("NU") {
                SheetButton(ButtonFamily.ICON, bare, BloxIcon.PLUS)
                SheetButton(ButtonFamily.ICON, bare.copy(depth = 1f), BloxIcon.PLUS)
                SheetButton(ButtonFamily.ICON, bare.copy(enabled = false), BloxIcon.PLUS)
            }
            VariantLine("COMPACT") {
                SheetButton(ButtonFamily.PRIMARY, ButtonState(), BloxIcon.CHECK, "Enregistrer", compact = true)
                SheetButton(ButtonFamily.SECONDARY, ButtonState(), BloxIcon.UNDO, "Annuler", compact = true)
                SheetButton(
                    ButtonFamily.SECONDARY,
                    ButtonState(),
                    BloxIcon.ALERT,
                    "Tuile",
                    compact = true,
                    shape = BloxTheme.shapes.md,
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 560)
@Composable
internal fun ButtonVariantsOriginalPreview() {
    ButtonVariantsSheet(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 560)
@Composable
internal fun ButtonVariantsColorfulPreview() {
    ButtonVariantsSheet(BloxThemeKind.COLORFUL)
}
