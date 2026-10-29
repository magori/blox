package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.locksmith.PadlockResult

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/**
 * Padlocks 1 to 12 open, 3 of them perfectly (1, 4 and 9), as « 12 ouvertes · 3 parfaites » in the rules: Reprendre
 * opens padlock 13, the frontier, among the craftsmen, its row completed by locked padlocks.
 */
private val twelveOpen = PadlocksUiState(
    (1..12).map { level ->
        val pieces = if (level <= 3) level + 2 else 6
        if (level in setOf(1, 4, 9)) {
            PadlockResult(pieces = pieces, moves = pieces, refusals = 0, hints = 0)
        } else {
            PadlockResult(pieces = pieces, moves = pieces + 1 + level % 3, refusals = level % 2, hints = level % 4 / 3)
        }
    },
)

/** The screen of [state], the player coming from padlock [current], under the Original theme of the application. */
@Composable
private fun PadlocksPreview(
    state: PadlocksUiState,
    current: Int? = null,
) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        PadlocksScreen(state = state, current = current, actions = PadlocksActions.None)
    }
}

/** 12 open, 3 perfect, the player coming from padlock 5, an old one replayed. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PadlocksDayPreview() {
    PadlocksPreview(twelveOpen, current = 5)
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun PadlocksNightPreview() {
    PadlocksPreview(twelveOpen, current = 5)
}

/** No padlock open yet: « Ouvre la serrure 1 pour commencer. », padlock 1 to open, no Reprendre. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PadlocksEmptyDayPreview() {
    PadlocksPreview(PadlocksUiState(emptyList()))
}

/** A small screen in large text: two columns, the summary and Reprendre scrolled away with the grid, on the frontier. */
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
internal fun PadlocksSmallDayPreview() {
    PadlocksPreview(twelveOpen, current = 13)
}

/** A screen as low as a phone in landscape, in large text: a whole row of tiles still shows, the frontier. */
@Preview(widthDp = 360, heightDp = 400, fontScale = 2f)
@Composable
internal fun PadlocksLowDayPreview() {
    PadlocksPreview(twelveOpen, current = 13)
}

/**
 * In large text, the longest words on a tile: padlock 12, perfect and the one the player comes from, « parfaite » and
 * « en cours », beside 11, open, the frontier and a locked padlock.
 */
@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun PadlocksLargeFontDayPreview() {
    val perfect = PadlockResult(pieces = 6, moves = 6, refusals = 0, hints = 0)
    PadlocksPreview(PadlocksUiState(twelveOpen.openings.dropLast(1) + perfect), current = 12)
}
