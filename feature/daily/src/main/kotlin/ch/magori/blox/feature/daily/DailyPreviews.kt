package ch.magori.blox.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.game.model.PieceType
import java.time.LocalDate

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** The 26th of October 2026 and its seven first pieces (seed 20261026), before any try. */
private val beforeAnyTry = DailyUiState(
    date = LocalDate.of(2026, 10, 26),
    firstPieces = "ZTIZOZJ".map { PieceType.valueOf("$it") },
    startLevel = 5,
    tries = 0,
    firstTry = null,
    best = null,
)

/** The same day, after five tries. */
private val afterTries = beforeAnyTry.copy(tries = 5, firstTry = 9_800, best = 18_200)

/** The entry under the Original theme of the application: it draws itself in the Colorful theme. */
@Composable
private fun DailyPreview(uiState: DailyUiState) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        DailyScreen(uiState = uiState, actions = DailyActions.None)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun DailyBeforeAnyTryPreview() = DailyPreview(beforeAnyTry)

@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
internal fun DailyBeforeAnyTryNightPreview() = DailyPreview(beforeAnyTry)

@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun DailyAfterTriesPreview() = DailyPreview(afterTries)

@Preview(widthDp = 360, heightDp = 800, uiMode = UI_MODE_NIGHT)
@Composable
internal fun DailyAfterTriesNightPreview() = DailyPreview(afterTries)

@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
internal fun DailyLargeFontPreview() = DailyPreview(afterTries)
