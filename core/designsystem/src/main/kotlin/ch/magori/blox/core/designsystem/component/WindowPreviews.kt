package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind

/** A window over a page of the theme [kind], as it shows over a screen, without the window of the system. */
@Composable
private fun WindowPreview(
    kind: BloxThemeKind,
    window: @Composable () -> Unit,
) {
    BloxTheme(kind = kind) { WindowOverPage(window = window) }
}

/** A name taken: its problem told under the field, the free name offered, the confirmation disabled. */
@Composable
private fun TakenNameWindow() {
    NamingWindow(
        title = "Renommer « Thème du soir »",
        name = "Ma marche",
        onNameChanged = {},
        maxLength = 40,
        problem = "Une autre musique porte déjà ce nom.",
        confirm = "Renommer",
        onConfirm = {},
        onDismiss = {},
        freeName = "Prendre « Ma marche 2 »",
    )
}

/** A deletion that could not be written, with its note. */
@Composable
private fun FailedDeleteWindow() {
    DeleteWindow(
        name = "Thème du soir",
        keep = "Garder la musique",
        onConfirm = {},
        onDismiss = {},
        note = "Elle est jouée pendant les parties : la musique choisie à l'accueil la remplacera.",
        failed = true,
    )
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun NamingWindowPreview() {
    WindowPreview(BloxThemeKind.COLORFUL) { TakenNameWindow() }
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun NamingWindowLargeFontPreview() {
    WindowPreview(BloxThemeKind.COLORFUL) { TakenNameWindow() }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun DeleteWindowPreview() {
    WindowPreview(BloxThemeKind.COLORFUL) { FailedDeleteWindow() }
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun DeleteWindowLargeFontPreview() {
    WindowPreview(BloxThemeKind.COLORFUL) { FailedDeleteWindow() }
}
